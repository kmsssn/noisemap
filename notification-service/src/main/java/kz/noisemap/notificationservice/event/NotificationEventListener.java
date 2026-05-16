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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    private static final double NOISE_ALERT_THRESHOLD_DBA = 85.0;

    @Value("${app.moderation.moderator-ids:}")
    private String moderatorIdsRaw;

    @RabbitListener(queues = RabbitConstants.Q_NOTIFICATION_ACHIEVEMENT)
    public void handleAchievementUnlocked(AchievementUnlockedEvent event) {
        log.info("Notification: achievement {} for user {}", event.getAchievementCode(), event.getUserId());
        try {
            Map<String, Object> metadata = Map.of(
                    "achievementCode", event.getAchievementCode(),
                    "pointsAwarded",   event.getPointsAwarded()
            );
            notificationService.createNotification(
                    event.getUserId(),
                    NotificationType.ACHIEVEMENT_UNLOCKED,
                    "ACHIEVEMENT_UNLOCKED",
                    "ACHIEVEMENT_UNLOCKED_MESSAGE",
                    metadata
            );
        } catch (Exception e) {
            log.error("Failed to create achievement notification", e);
        }
    }

    @RabbitListener(queues = RabbitConstants.Q_NOTIFICATION_NOISE_ALERT)
    public void handleNoiseAlert(ClassificationCompletedEvent event) {
        if (event.getNoiseLevelDba() == null || event.getNoiseLevelDba() < NOISE_ALERT_THRESHOLD_DBA) {
            return;
        }
        try {
            notificationService.createNotification(
                    event.getUserId(),
                    NotificationType.NOISE_ALERT,
                    "NOISE_ALERT",
                    "NOISE_ALERT_MESSAGE",
                    Map.of("noiseLevelDba", event.getNoiseLevelDba(),
                            "noiseClass",    event.getNoiseClass() != null ? event.getNoiseClass() : "")
            );
        } catch (Exception e) {
            log.error("Failed to create noise alert notification", e);
        }
    }

    @RabbitListener(queues = RabbitConstants.Q_NOTIFICATION_MODERATOR)
    public void handleRecordingFlagged(RecordingFlaggedEvent event) {
        log.info("Notification: recording {} flagged, reason: {}", event.getRecordingId(), event.getReason());

        try {
            notificationService.createNotification(
                    event.getUserId(),
                    NotificationType.RECORDING_FLAGGED,
                    "RECORDING_FLAGGED",
                    "RECORDING_FLAGGED_MESSAGE",
                    Map.of("reason", event.getReason() != null ? event.getReason() : "")
            );
        } catch (Exception e) {
            log.error("Failed to notify user {} about flagged recording", event.getUserId(), e);
        }

        List<UUID> moderators = parseModerators();
        if (moderators.isEmpty()) {
            log.debug("No moderators configured in app.moderation.moderator-ids — skipping moderator notifications");
            return;
        }

        for (UUID moderatorId : moderators) {
            try {
                notificationService.createNotification(
                        moderatorId,
                        NotificationType.MODERATION_ALERT,
                        "MODERATION_ALERT",
                        "MODERATION_ALERT_MESSAGE",
                        Map.of("recordingId", event.getRecordingId(),
                                "reason",      event.getReason() != null ? event.getReason() : "")
                );
            } catch (Exception e) {
                log.error("Failed to notify moderator {} about flagged recording", moderatorId, e);
            }
        }
    }

    private List<UUID> parseModerators() {
        if (moderatorIdsRaw == null || moderatorIdsRaw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(moderatorIdsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> {
                    try {
                        return UUID.fromString(s);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid moderator UUID in config: '{}'", s);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}