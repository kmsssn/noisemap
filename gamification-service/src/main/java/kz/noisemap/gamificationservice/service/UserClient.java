package kz.noisemap.gamificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * REST клиент для получения данных пользователя из user-service.
 * Используется в лидерборде для получения displayName.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserClient {

    private final RestTemplate restTemplate;

    @Value("${services.user-service.url:http://user-service:8081}")
    private String userServiceUrl;

    /**
     * Получить displayName пользователя.
     * При ошибке возвращает "User {id}" чтобы не ломать лидерборд.
     */
    public String getDisplayName(UUID userId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> user = restTemplate.getForObject(
                    userServiceUrl + "/api/v1/users/" + userId + "/public",
                    Map.class
            );
            if (user != null && user.containsKey("displayName")) {
                return (String) user.get("displayName");
            }
        } catch (Exception e) {
            log.warn("Could not fetch displayName for user {}: {}", userId, e.getMessage());
        }
        return "User " + userId.toString().substring(0, 8);
    }
}
