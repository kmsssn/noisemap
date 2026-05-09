package kz.noisemap.notificationservice.event;

import kz.noisemap.common.event.AchievementUnlockedEvent;
import kz.noisemap.common.event.ClassificationCompletedEvent;
import kz.noisemap.common.event.RabbitConstants;
import kz.noisemap.common.event.RecordingFlaggedEvent;
import kz.noisemap.notificationservice.model.NotificationType;
import kz.noisemap.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    private static final double NOISE_ALERT_THRESHOLD_DBA = 85.0;

    /**
     * Achievement unlocked → push to user.
     */
    @RabbitListener(queues = RabbitConstants.Q_NOTIFICATION_ACHIEVEMENT)
    public void handleAchievementUnlocked(AchievementUnlockedEvent event) {
        log.info("Notification: achievement {} for user {}", event.getAchievementCode(), event.getUserId());

        try {
            notificationService.createNotification(
                    event.getUserId(),
                    NotificationType.ACHIEVEMENT_UNLOCKED,
                    "New achievement: " + event.getAchievementTitle(),
                    "You earned +" + event.getPointsAwarded() + " points!"
            );
        } catch (Exception e) {
            log.error("Failed to create achievement notification", e);
        }
    }

    /**
     * High noise level detected → send alert notification.
     */
    @RabbitListener(queues = RabbitConstants.Q_NOTIFICATION_NOISE_ALERT)
    public void handleNoiseAlert(ClassificationCompletedEvent event) {
        if (event.getNoiseLevelDba() == null || event.getNoiseLevelDba() < NOISE_ALERT_THRESHOLD_DBA) {
            return;
        }

        try {
            notificationService.createNotification(
                    event.getUserId(),
                    NotificationType.NOISE_ALERT,
                    "High Noise Level!",
                    String.format("Recorded %.1f dBA. Noise type: %s. Hearing protection recommended.",
                            event.getNoiseLevelDba(), event.getNoiseClass())
            );
        } catch (Exception e) {
            log.error("Failed to create noise alert notification", e);
        }
    }

    /**
     * Recording flagged by moderation → notify moderators.
     */
    @RabbitListener(queues = RabbitConstants.Q_NOTIFICATION_MODERATOR)
    public void handleRecordingFlagged(RecordingFlaggedEvent event) {
        log.info("Notification: recording {} flagged, reason: {}", event.getRecordingId(), event.getReason());

        // TODO: notify all moderators (fetch list from User Service)
        try {
            notificationService.createNotification(
                    event.getUserId(),
                    NotificationType.RECORDING_FLAGGED,
                    "Recording Flagged for Review",
                    "Your recording has been sent for moderation. Reason: " + event.getReason()
            );
        } catch (Exception e) {
            log.error("Failed to create flagged notification", e);
        }
    }
}
