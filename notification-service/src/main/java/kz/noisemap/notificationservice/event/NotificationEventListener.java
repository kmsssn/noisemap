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
     * Ачивка разблокирована → push пользователю.
     */
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

    /**
     * Высокий уровень шума → уведомление.
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
                    "Высокий уровень шума!",
                    String.format("Зафиксировано %.1f дБА. Тип шума: %s. Рекомендуется защита слуха.",
                            event.getNoiseLevelDba(), event.getNoiseClass())
            );
        } catch (Exception e) {
            log.error("Failed to create noise alert notification", e);
        }
    }

    /**
     * Запись помечена модерацией → уведомление модераторам.
     */
    @RabbitListener(queues = RabbitConstants.Q_NOTIFICATION_MODERATOR)
    public void handleRecordingFlagged(RecordingFlaggedEvent event) {
        log.info("Notification: recording {} flagged, reason: {}", event.getRecordingId(), event.getReason());

        // TODO: уведомить всех модераторов (получить список из User Service)
        // Пока логируем — в продакшене здесь будет вызов User Service для получения модераторов
        try {
            notificationService.createNotification(
                    event.getUserId(),
                    NotificationType.RECORDING_FLAGGED,
                    "Запись отмечена для проверки",
                    "Ваша запись отправлена на модерацию. Причина: " + event.getReason()
            );
        } catch (Exception e) {
            log.error("Failed to create flagged notification", e);
        }
    }
}
