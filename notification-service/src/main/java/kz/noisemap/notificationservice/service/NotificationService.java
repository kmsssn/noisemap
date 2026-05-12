package kz.noisemap.notificationservice.service;

import kz.noisemap.notificationservice.dto.NotificationDto;
import kz.noisemap.notificationservice.model.Notification;
import kz.noisemap.notificationservice.model.NotificationType;
import kz.noisemap.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // Раскомментировать и внедрить FcmService когда появятся Firebase credentials.
    // private final FcmService fcmService;

    /**
     * Создать in-app уведомление и (в будущем) отправить push через FCM.
     */
    public void createNotification(UUID userId, NotificationType type, String title, String message) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);
        log.info("Notification created: userId={}, type={}, title={}", userId, type, title);

        // FCM Push — раскомментировать после интеграции Firebase Admin SDK:
        // fcmService.sendToUser(userId, title, message);
    }

    public org.springframework.data.domain.Page<NotificationDto.Response> getUserNotifications(
            UUID userId, org.springframework.data.domain.Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public void markAsRead(String notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Отмечает все непрочитанные уведомления пользователя как прочитанные.
     * Использует bulk MongoDB update — один запрос вместо N save().
     */
    public void markAllAsRead(UUID userId) {
        long updated = notificationRepository.findAndUpdateByUserIdAndReadFalse(userId);
        log.debug("Marked {} notifications as read for userId={}", updated, userId);
    }

    private NotificationDto.Response toResponse(Notification notification) {
        return NotificationDto.Response.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .read(notification.getRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}