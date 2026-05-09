package kz.noisemap.notificationservice.model;

public enum NotificationType {
    ACHIEVEMENT_UNLOCKED,
    NOISE_ALERT,           // high noise in a saved area
    RECORDING_FLAGGED,     // recording flagged by moderation
    MODERATION_ALERT,      // for moderators
    WEEKLY_DIGEST          // weekly report
}
