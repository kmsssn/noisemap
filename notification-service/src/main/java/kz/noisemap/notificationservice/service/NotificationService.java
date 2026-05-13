package kz.noisemap.notificationservice.service;

import kz.noisemap.notificationservice.dto.NotificationDto;
import kz.noisemap.notificationservice.model.Notification;
import kz.noisemap.notificationservice.model.NotificationType;
import kz.noisemap.notificationservice.repository.NotificationRepository;
import kz.noisemap.notificationservice.websocket.WebSocketNotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WebSocketNotificationPublisher webSocketPublisher;

    /**
     * Создать уведомление в MongoDB и отправить по WebSocket в реальном времени.
     */
    public void createNotification(UUID userId, NotificationType type, String title, String message) {
        // 1. Сохранить в MongoDB
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .createdAt(Instant.now())
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created: userId={}, type={}, title={}", userId, type, title);

        // 2. Отправить по WebSocket (если юзер online)
        NotificationDto.Response response = toResponse(notification);
        webSocketPublisher.sendNotification(userId, response);
    }

    public Page<NotificationDto.Response> getUserNotifications(UUID userId, Pageable pageable) {
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