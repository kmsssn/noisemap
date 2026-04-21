package kz.noisemap.gamificationservice.service;

import kz.noisemap.common.event.AchievementUnlockedEvent;
import kz.noisemap.common.event.ClassificationCompletedEvent;
import kz.noisemap.common.event.RabbitConstants;
import kz.noisemap.common.event.RecordingCreatedEvent;
import kz.noisemap.gamificationservice.dto.GamificationDto;
import kz.noisemap.gamificationservice.model.AchievementDefinition;
import kz.noisemap.gamificationservice.model.UserAchievement;
import kz.noisemap.gamificationservice.model.UserScore;
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
import java.util.ArrayList;
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
    private final RabbitTemplate rabbitTemplate;

    private static final int POINTS_PER_RECORDING = 10;
    private static final int POINTS_PER_LEVEL = 100;

    /**
     * Вызывается при recording.created — начислить очки за запись.
     */
    @Transactional
    public void handleNewRecording(RecordingCreatedEvent event) {
        UUID userId = event.getUserId();

        UserScore score = scoreRepository.findById(userId)
                .orElse(UserScore.builder()
                        .userId(userId)
                        .totalPoints(0)
                        .totalRecordings(0)
                        .level(1)
                        .currentStreak(0)
                        .build());

        score.setTotalPoints(score.getTotalPoints() + POINTS_PER_RECORDING);
        score.setTotalRecordings(score.getTotalRecordings() + 1);
        score.setLevel(score.getTotalPoints() / POINTS_PER_LEVEL + 1);

        // Стрик: если последняя запись была вчера — продолжаем, иначе сбрасываем
        updateStreak(score, event.getRecordedAt());

        score.setLastRecordingDate(event.getRecordedAt());
        scoreRepository.save(score);

        // Проверить ачивки по количеству записей
        checkRecordingAchievements(userId, score);
        checkTimeAchievements(userId, event.getRecordedAt());
        checkStreakAchievements(userId, score);
    }

    /**
     * Вызывается при classification.completed — проверить ачивки по дБА.
     */
    @Transactional
    public void handleClassificationResult(ClassificationCompletedEvent event) {
        UUID userId = event.getUserId();

        if (event.getNoiseLevelDba() != null && event.getNoiseLevelDba() < 40) {
            tryUnlockAchievement(userId, AchievementDefinition.QUIET_FINDER);
        }
        if (event.getNoiseLevelDba() != null && event.getNoiseLevelDba() > 85) {
            tryUnlockAchievement(userId, AchievementDefinition.LOUD_DISCOVERER);
        }
    }

    public GamificationDto.ProfileResponse getProfile(UUID userId) {
        UserScore score = scoreRepository.findById(userId)
                .orElse(UserScore.builder()
                        .userId(userId)
                        .totalPoints(0)
                        .totalRecordings(0)
                        .level(1)
                        .currentStreak(0)
                        .build());

        List<GamificationDto.AchievementResponse> achievements = achievementRepository.findByUserId(userId)
                .stream()
                .map(a -> GamificationDto.AchievementResponse.builder()
                        .code(a.getAchievementCode())
                        .title(a.getAchievementTitle())
                        .description(a.getDescription())
                        .pointsAwarded(a.getPointsAwarded())
                        .unlockedAt(a.getUnlockedAt())
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

    public List<GamificationDto.LeaderboardEntry> getLeaderboard(int limit) {
        List<UserScore> topScores = scoreRepository.findTopByPoints(PageRequest.of(0, limit));

        AtomicInteger rank = new AtomicInteger(1);
        return topScores.stream()
                .map(s -> GamificationDto.LeaderboardEntry.builder()
                        .rank(rank.getAndIncrement())
                        .userId(s.getUserId())
                        .totalPoints(s.getTotalPoints())
                        .totalRecordings(s.getTotalRecordings())
                        .level(s.getLevel())
                        .build())
                .collect(Collectors.toList());
    }

    // === Приватные методы ===

    private void updateStreak(UserScore score, Instant recordedAt) {
        if (score.getLastRecordingDate() == null) {
            score.setCurrentStreak(1);
            return;
        }

        ZonedDateTime lastDate = score.getLastRecordingDate().atZone(ZoneId.of("Asia/Almaty"));
        ZonedDateTime currentDate = recordedAt.atZone(ZoneId.of("Asia/Almaty"));

        long daysBetween = java.time.Duration.between(
                lastDate.toLocalDate().atStartOfDay(),
                currentDate.toLocalDate().atStartOfDay()
        ).toDays();

        if (daysBetween == 1) {
            score.setCurrentStreak(score.getCurrentStreak() + 1);
        } else if (daysBetween > 1) {
            score.setCurrentStreak(1);
        }
        // daysBetween == 0 — тот же день, стрик не меняется
    }

    private void checkRecordingAchievements(UUID userId, UserScore score) {
        int count = score.getTotalRecordings();
        if (count >= 1) tryUnlockAchievement(userId, AchievementDefinition.FIRST_RECORDING);
        if (count >= 10) tryUnlockAchievement(userId, AchievementDefinition.RECORDINGS_10);
        if (count >= 50) tryUnlockAchievement(userId, AchievementDefinition.RECORDINGS_50);
        if (count >= 100) tryUnlockAchievement(userId, AchievementDefinition.RECORDINGS_100);
        if (count >= 500) tryUnlockAchievement(userId, AchievementDefinition.RECORDINGS_500);
    }

    private void checkTimeAchievements(UUID userId, Instant recordedAt) {
        ZonedDateTime time = recordedAt.atZone(ZoneId.of("Asia/Almaty"));
        int hour = time.getHour();

        if (hour >= 23 || hour < 5) {
            tryUnlockAchievement(userId, AchievementDefinition.NIGHT_OWL);
        }
        if (hour >= 5 && hour < 7) {
            tryUnlockAchievement(userId, AchievementDefinition.EARLY_BIRD);
        }
    }

    private void checkStreakAchievements(UUID userId, UserScore score) {
        if (score.getCurrentStreak() >= 7) tryUnlockAchievement(userId, AchievementDefinition.STREAK_7);
        if (score.getCurrentStreak() >= 30) tryUnlockAchievement(userId, AchievementDefinition.STREAK_30);
    }

    private void tryUnlockAchievement(UUID userId, AchievementDefinition definition) {
        if (achievementRepository.existsByUserIdAndAchievementCode(userId, definition.getCode())) {
            return; // Уже получена
        }

        UserAchievement achievement = UserAchievement.builder()
                .userId(userId)
                .achievementCode(definition.getCode())
                .achievementTitle(definition.getTitle())
                .description(definition.getDescription())
                .pointsAwarded(definition.getPoints())
                .unlockedAt(Instant.now())
                .build();

        achievementRepository.save(achievement);

        // Добавить бонусные очки за ачивку
        scoreRepository.findById(userId).ifPresent(score -> {
            score.setTotalPoints(score.getTotalPoints() + definition.getPoints());
            score.setLevel(score.getTotalPoints() / POINTS_PER_LEVEL + 1);
            scoreRepository.save(score);
        });

        // Опубликовать событие → Notification Service
        AchievementUnlockedEvent event = AchievementUnlockedEvent.builder()
                .userId(userId)
                .achievementCode(definition.getCode())
                .achievementTitle(definition.getTitle())
                .pointsAwarded(definition.getPoints())
                .unlockedAt(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitConstants.EXCHANGE,
                RabbitConstants.RK_ACHIEVEMENT_UNLOCKED,
                event
        );

        log.info("Achievement unlocked: userId={}, achievement={}", userId, definition.getCode());
    }
}
