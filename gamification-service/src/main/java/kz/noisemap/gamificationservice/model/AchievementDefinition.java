package kz.noisemap.gamificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Справочник всех ачивок. Добавляем новые сюда.
 */
@Getter
@AllArgsConstructor
public enum AchievementDefinition {

    FIRST_RECORDING("first_recording", "Первый шаг", "Сделайте первую запись", 10),
    RECORDINGS_10("recordings_10", "Активист", "Сделайте 10 записей", 50),
    RECORDINGS_50("recordings_50", "Исследователь", "Сделайте 50 записей", 200),
    RECORDINGS_100("recordings_100", "Эксперт", "Сделайте 100 записей", 500),
    RECORDINGS_500("recordings_500", "Легенда", "Сделайте 500 записей", 2000),

    QUIET_FINDER("quiet_finder", "Тишина и покой", "Найдите место тише 40 дБА", 100),
    LOUD_DISCOVERER("loud_discoverer", "Горячая точка", "Найдите место громче 85 дБА", 50),

    NIGHT_OWL("night_owl", "Ночной дозор", "Сделайте запись между 23:00 и 5:00", 75),
    EARLY_BIRD("early_bird", "Ранняя пташка", "Сделайте запись между 5:00 и 7:00", 75),

    STREAK_7("streak_7", "Неделя подряд", "Записывайте звук 7 дней подряд", 150),
    STREAK_30("streak_30", "Месяц подряд", "Записывайте звук 30 дней подряд", 500);

    private final String code;
    private final String title;
    private final String description;
    private final int points;
}
