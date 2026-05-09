package kz.noisemap.gamificationservice.service;

import kz.noisemap.common.event.AchievementUnlockedEvent;
import kz.noisemap.common.event.ClassificationCompletedEvent;
import kz.noisemap.common.event.RabbitConstants;
import kz.noisemap.common.event.RecordingCreatedEvent;
import kz.noisemap.gamificationservice.dto.GamificationDto;
import kz.noisemap.gamificationservice.model.AchievementDefinition;
import kz.noisemap.gamificationservice.model.UserAchievement;
import kz.noisemap.gamificationservice.model.UserScore;
import kz.noisemap.gamificationservice.repository.AchievementDefinitionRepository;
import kz.noisemap.gamificationservice.repository.UserAchievementRepository;
import kz.noisemap.gamificationservice.repository.UserScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserScoreRepository scoreRepository;
    private final UserAchievementRepository achievementRepository;
    private final AchievementDefinitionRepository definitionRepository;
    private final AchievementCacheService cacheService;
    private final AchievementEngine achievementEngine;
    private final UserClient userClient;
    private final RabbitTemplate rabbitTemplate;

    private static final int POINTS_PER_RECORDING = 10;
    private static final int POINTS_PER_LEVEL = 100;
    private static final String TZ = "Asia/Almaty";

    @Transactional
    public void handleNewRecording(RecordingCreatedEvent event) {
        UUID userId = event.getUserId();

        // Получить или создать счёт пользователя
        UserScore score = scoreRepository.findById(userId)
                .orElse(UserScore.builder()
                        .userId(userId)
                        .totalPoints(0)
                        .totalRecordings(0)
                        .level(1)
                        .currentStreak(0)
                        .build());

        // Обновить очки и статистику
        score.setTotalPoints(score.getTotalPoints() + POINTS_PER_RECORDING);
        score.setTotalRecordings(score.getTotalRecordings() + 1);
        updateStreak(score, event.getRecordedAt());
        score.setLastRecordingDate(event.getRecordedAt());
        score = scoreRepository.save(score);

        // Проверить и выдать ачивки (2 запроса в БД)
        ZonedDateTime recordedAt = event.getRecordedAt().atZone(ZoneId.of(TZ));
        List<AchievementDefinition> toUnlock =
                achievementEngine.checkAfterRecording(userId, score, recordedAt);

        toUnlock.forEach(def -> unlockAchievement(userId, def));

        // Пересчитать уровень после бонусных очков за ачивки
        UserScore finalScore = scoreRepository.findById(userId).orElse(score);
        finalScore.setLevel(finalScore.getTotalPoints() / POINTS_PER_LEVEL + 1);
        scoreRepository.save(finalScore);
    }

    @Transactional
    public void handleClassificationResult(ClassificationCompletedEvent event) {
        achievementEngine
                .checkAfterClassification(event.getUserId(), event.getNoiseLevelDba())
                .forEach(def -> unlockAchievement(event.getUserId(), def));
    }

    /**
     * Профиль пользователя — JOIN FETCH за один запрос.
     * Нет N+1 проблемы при загрузке ачивок с определениями.
     */
    public GamificationDto.ProfileResponse getProfile(UUID userId) {
        UserScore score = scoreRepository.findById(userId)
                .orElse(UserScore.builder()
                        .userId(userId).totalPoints(0).totalRecordings(0)
                        .level(1).currentStreak(0).build());

        // JOIN FETCH — один запрос вместо N+1
        List<GamificationDto.AchievementResponse> achievements =
                achievementRepository.findByUserIdWithDefinitions(userId)
                        .stream()
                        .map(ua -> GamificationDto.AchievementResponse.builder()
                                .code(ua.getAchievementCode())
                                .title(ua.getDefinition().getTitle())
                                .description(ua.getDefinition().getDescription())
                                .pointsAwarded(ua.getDefinition().getPoints())
                                .iconUrl(ua.getDefinition().getIconUrl())
                                .unlockedAt(ua.getUnlockedAt())
                                .build())
                        .collect(Collectors.toList());

        return GamificationDto.ProfileResponse.builder()
                .userId(userId)
                .totalPoints(score.getTotalPoints())
                .totalRecordings(score.getTotalRecordings())
                .level(score.getLevel())
                .currentStreak(score.getCurrentStreak())
                .achievements(achievements)
                .build();
    }

    /**
     * Лидерборд с именами пользователей.
     */
    public List<GamificationDto.LeaderboardEntry> getLeaderboard(int limit) {
        List<UserScore> top = scoreRepository.findTopByPoints(PageRequest.of(0, limit));
        AtomicInteger rank = new AtomicInteger(1);
        return top.stream()
                .map(s -> GamificationDto.LeaderboardEntry.builder()
                        .rank(rank.getAndIncrement())
                        .userId(s.getUserId())
                        .displayName(userClient.getDisplayName(s.getUserId()))
                        .totalPoints(s.getTotalPoints())
                        .totalRecordings(s.getTotalRecordings())
                        .level(s.getLevel())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Каталог всех ачивок — из кэша.
     */
    public List<GamificationDto.AchievementDefinitionResponse> getAllDefinitions() {
        return cacheService.getAllActive().stream()
                .map(def -> GamificationDto.AchievementDefinitionResponse.builder()
                        .code(def.getCode())
                        .title(def.getTitle())
                        .description(def.getDescription())
                        .points(def.getPoints())
                        .triggerType(def.getTriggerType())
                        .triggerValue(def.getTriggerValue())
                        .iconUrl(def.getIconUrl())
                        .active(def.getActive())
                        .build())
                .collect(Collectors.toList());
    }

    // === Приватные методы ===

    private void unlockAchievement(UUID userId, AchievementDefinition def) {
        // Двойная проверка — на случай гонки потоков
        if (achievementRepository.existsByUserIdAndAchievementCode(userId, def.getCode())) return;

        achievementRepository.save(UserAchievement.builder()
                .userId(userId)
                .achievementCode(def.getCode())
                .unlockedAt(Instant.now())
                .build());

        // Бонусные очки
        scoreRepository.findById(userId).ifPresent(score -> {
            score.setTotalPoints(score.getTotalPoints() + def.getPoints());
            score.setLevel(score.getTotalPoints() / POINTS_PER_LEVEL + 1);
            scoreRepository.save(score);
        });

        // Событие → Notification Service
        rabbitTemplate.convertAndSend(
                RabbitConstants.EXCHANGE,
                RabbitConstants.RK_ACHIEVEMENT_UNLOCKED,
                AchievementUnlockedEvent.builder()
                        .userId(userId)
                        .achievementCode(def.getCode())
                        .achievementTitle(def.getTitle())
                        .pointsAwarded(def.getPoints())
                        .unlockedAt(Instant.now())
                        .build());

        log.info("Achievement unlocked: userId={}, code={}, +{}pts",
                userId, def.getCode(), def.getPoints());
    }

    private void updateStreak(UserScore score, java.time.Instant recordedAt) {
        if (score.getLastRecordingDate() == null) {
            score.setCurrentStreak(1);
            return;
        }
        ZonedDateTime last = score.getLastRecordingDate().atZone(ZoneId.of(TZ));
        ZonedDateTime current = recordedAt.atZone(ZoneId.of(TZ));
        long days = java.time.Duration.between(
                last.toLocalDate().atStartOfDay(),
                current.toLocalDate().atStartOfDay()).toDays();
        if (days == 1) score.setCurrentStreak(score.getCurrentStreak() + 1);
        else if (days > 1) score.setCurrentStreak(1);
        // days == 0 — тот же день, стрик не меняется
    }
}
