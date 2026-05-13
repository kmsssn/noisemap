package kz.noisemap.notificationservice.websocket;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Перехватчик WebSocket сообщений на этапе CONNECT.
 *
 * Извлекает JWT из header'а "Authorization: Bearer ..." при подключении,
 * валидирует подпись, парсит userId из claim "sub", устанавливает Principal
 * в сессии.
 *
 * После этого Spring может маршрутизировать сообщения через /user/{userId}/queue/*
 *
 * Если токен невалидный — подключение отклоняется.
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Только при CONNECT проверяем токен. Для остальных команд уже есть Principal в сессии.
            return message;
        }

        // Извлечь Authorization header
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            log.warn("WebSocket CONNECT without Authorization header — rejecting");
            return null; // отклонить подключение
        }

        String authHeader = authHeaders.get(0);
        if (!authHeader.startsWith("Bearer ")) {
            log.warn("WebSocket CONNECT with malformed Authorization — rejecting");
            return null;
        }

        String token = authHeader.substring("Bearer ".length());

        try {
            // Валидация JWT
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID userId = UUID.fromString(claims.getSubject());

            // Устанавливаем Principal в сессию WebSocket
            // Это позволит Spring маршрутизировать сообщения через /user/queue/*
            accessor.setUser(new WebSocketUserPrincipal(userId));

            log.info("WebSocket authenticated: userId={}", userId);

        } catch (Exception e) {
            log.warn("WebSocket CONNECT with invalid token: {} — rejecting", e.getMessage());
            return null;
        }

        return message;
    }

    /**
     * Principal для WebSocket сессии — содержит userId.
     * Используется Spring для маршрутизации /user/{userId}/queue/*
     */
    public static class WebSocketUserPrincipal implements Principal {
        private final UUID userId;

        public WebSocketUserPrincipal(UUID userId) {
            this.userId = userId;
        }

        @Override
        public String getName() {
            return userId.toString();
        }

        public UUID getUserId() {
            return userId;
        }
    }
}
