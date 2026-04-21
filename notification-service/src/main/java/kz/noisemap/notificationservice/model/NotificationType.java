package kz.noisemap.notificationservice.model;

public enum NotificationType {
    ACHIEVEMENT_UNLOCKED,
    NOISE_ALERT,           // высокий шум в сохранённом районе
    RECORDING_FLAGGED,     // запись помечена модерацией
    MODERATION_ALERT,      // для модераторов
    WEEKLY_DIGEST          // еженедельный отчёт
}
