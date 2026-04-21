package kz.noisemap.notificationservice.service;

import kz.noisemap.notificationservice.dto.NotificationDto;
import kz.noisemap.notificationservice.model.Notification;
import kz.noisemap.notificationservice.model.NotificationType;
import kz.noisemap.notificationservice.repository.NotificationRepository;
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

        // TODO: интеграция с Firebase Cloud Messaging для push-уведомлений
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
            throw new IllegalArgumentException("Access denied");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void markAllAsRead(UUID userId) {
        Page<Notification> unread = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged());

        unread.getContent().stream()
                .filter(n -> !n.getRead())
                .forEach(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
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
