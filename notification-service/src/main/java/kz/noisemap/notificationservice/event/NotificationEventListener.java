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
            notificationService.createNotification(
                    event.getUserId(),
                    NotificationType.ACHIEVEMENT_UNLOCKED,
                    "Новая ачивка: " + event.getAchievementTitle(),
                    "Вы получили +" + event.getPointsAwarded() + " очков!"
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
                    "Высокий уровень шума!",
                    String.format("Зафиксировано %.1f дБА. Тип шума: %s. Рекомендуется защита слуха.",
                            event.getNoiseLevelDba(), event.getNoiseClass())
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
                    "Запись отправлена на проверку",
                    "Ваша запись проходит проверку качества. Причина: " + event.getReason()
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
                        "Новая запись в очереди модерации",
                        String.format("Запись %s помечена как подозрительная. Причина: %s",
                                event.getRecordingId(), event.getReason())
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