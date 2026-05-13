package kz.noisemap.notificationservice.websocket;

import kz.noisemap.notificationservice.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Отправляет уведомления через WebSocket конкретному пользователю.
 *
 * Использует SimpMessagingTemplate.convertAndSendToUser() — Spring
 * автоматически маршрутизирует сообщение всем активным WebSocket сессиям
 * указанного userId.
 *
 * Если пользователь offline (нет активной WebSocket сессии) — сообщение
 * просто не отправится. Это нормально: in-app уведомление уже сохранено
 * в MongoDB, при reconnect клиент сделает GET /api/v1/notifications.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketNotificationPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Отправить уведомление по WebSocket.
     *
     * Клиент подписывается на /user/queue/notifications.
     * Spring автоматически маршрутизирует сообщение по Principal сессии.
     *
     * @param userId UUID получателя
     * @param notification DTO уведомления (то же что в GET /api/v1/notifications)
     */
    public void sendNotification(UUID userId, NotificationDto.Response notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification
            );
            log.debug("WebSocket: notification sent to userId={}, type={}",
                    userId, notification.getType());
        } catch (Exception e) {
            // Fail-safe: ошибка WebSocket не должна ломать основной flow создания уведомления
            log.error("WebSocket: failed to send notification to userId={}: {}",
                    userId, e.getMessage());
        }
    }
}
