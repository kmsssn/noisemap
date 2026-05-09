package kz.noisemap.gamificationservice.service;

import kz.noisemap.gamificationservice.model.AchievementDefinition;
import kz.noisemap.gamificationservice.model.UserScore;
import kz.noisemap.gamificationservice.repository.UserAchievementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Движок проверки ачивок.
 *
 * Исправленная производительность — 2 запроса в БД вместо N+1:
 * 1. Все активные определения — из кэша (редко меняются)
 * 2. Все уже полученные ачивки пользователя — один SELECT
 * Проверка в памяти через Set.contains() — O(1).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementEngine {

    private final AchievementCacheService cacheService;
    private final UserAchievementRepository userAchievementRepository;

    private static final String TZ = "Asia/Almaty";

    /**
     * Проверить ачивки после новой записи.
     * Вызывается когда recording.created обработан.
     */
    public List<AchievementDefinition> checkAfterRecording(
            UUID userId,
            UserScore score,
            ZonedDateTime recordedAt) {

        // Запрос 1: все определения из кэша (не БД)
        List<AchievementDefinition> all = cacheService.getAllActive();

        // Запрос 2: все уже полученные ачивки пользователя — одним SELECT
        Set<String> alreadyUnlocked = loadUnlockedCodes(userId);

        List<AchievementDefinition> toUnlock = new ArrayList<>();

        for (AchievementDefinition def : all) {
            // Проверка в памяти — O(1), без запроса в БД
            if (alreadyUnlocked.contains(def.getCode())) continue;

            if (checkRecordingTrigger(def, score, recordedAt)) {
                toUnlock.add(def);
            }
        }

        return toUnlock;
    }

    /**
     * Проверить ачивки по уровню шума.
     * Вызывается когда classification.completed получен.
     */
    public List<AchievementDefinition> checkAfterClassification(
            UUID userId,
            Double noiseLevelDba) {

        if (noiseLevelDba == null) return List.of();

        List<AchievementDefinition> all = cacheService.getAllActive();
        Set<String> alreadyUnlocked = loadUnlockedCodes(userId);

        return all.stream()
                .filter(def -> !alreadyUnlocked.contains(def.getCode()))
                .filter(def -> checkNoiseTrigger(def, noiseLevelDba))
                .collect(Collectors.toList());
    }

    // === Приватные методы ===

    /**
     * Один запрос — все коды разблокированных ачивок пользователя.
     * Возвращает Set для O(1) проверки.
     */
    private Set<String> loadUnlockedCodes(UUID userId) {
        return userAchievementRepository.findByUserId(userId)
                .stream()
                .map(ua -> ua.getAchievementCode())
                .collect(Collectors.toSet());
    }

    private boolean checkRecordingTrigger(AchievementDefinition def,
                                           UserScore score,
                                           ZonedDateTime recordedAt) {
        return switch (def.getTriggerType()) {
            case "recording_count" -> {
                int required = Integer.parseInt(def.getTriggerValue());
                yield score.getTotalRecordings() >= required;
            }
            case "time_of_day" -> {
                int hour = recordedAt.withZoneSameInstant(ZoneId.of(TZ)).getHour();
                yield switch (def.getTriggerValue()) {
                    case "night"   -> hour >= 23 || hour < 5;
                    case "morning" -> hour >= 5 && hour < 7;
                    default -> false;
                };
            }
            case "streak_days" -> {
                int required = Integer.parseInt(def.getTriggerValue());
                yield score.getCurrentStreak() >= required;
            }
            default -> false; // noise триггеры проверяются в checkAfterClassification
        };
    }

    private boolean checkNoiseTrigger(AchievementDefinition def, double noiseLevelDba) {
        return switch (def.getTriggerType()) {
            case "noise_level_below" -> {
                double threshold = Double.parseDouble(def.getTriggerValue());
                yield noiseLevelDba < threshold;
            }
            case "noise_level_above" -> {
                double threshold = Double.parseDouble(def.getTriggerValue());
                yield noiseLevelDba > threshold;
            }
            default -> false;
        };
    }
}
