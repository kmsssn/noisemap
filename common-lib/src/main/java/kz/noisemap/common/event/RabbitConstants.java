package kz.noisemap.common.event;

public final class RabbitConstants {

    private RabbitConstants() {}

    // Exchange
    public static final String EXCHANGE = "noisemap.events";

    // Routing keys
    public static final String RK_RECORDING_CREATED = "recording.created";
    public static final String RK_CLASSIFICATION_COMPLETED = "classification.completed";
    public static final String RK_ACHIEVEMENT_UNLOCKED = "achievement.unlocked";
    public static final String RK_RECORDING_FLAGGED = "recording.flagged";

    // Queues — каждый потребитель слушает свою очередь
    public static final String Q_ML_CLASSIFICATION = "ml.classification.queue";
    public static final String Q_MODERATION_CHECK = "moderation.check.queue";
    public static final String Q_GAMIFICATION_RECORDING = "gamification.recording.queue";
    public static final String Q_GAMIFICATION_ACHIEVEMENT = "gamification.achievement.queue";
    public static final String Q_STATISTICS_UPDATE = "statistics.update.queue";
    public static final String Q_NOTIFICATION_ACHIEVEMENT = "notification.achievement.queue";
    public static final String Q_NOTIFICATION_MODERATOR = "notification.moderator.queue";
    public static final String Q_NOTIFICATION_NOISE_ALERT = "notification.noise.alert.queue";
}