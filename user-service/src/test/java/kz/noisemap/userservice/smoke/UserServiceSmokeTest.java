package kz.noisemap.userservice.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Smoke тест: проверяет что user-service стартует и health endpoint отвечает.
 *
 * Использует отдельный конфиг для тестов (TestPropertySource) — in-memory H2
 * вместо реального PostgreSQL.
 *
 * Запуск:
 *   mvn test -pl user-service -Dtest=UserServiceSmokeTest
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // H2 in-memory вместо PostgreSQL
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        // Отключить mail для тестов
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "spring.mail.username=test",
        "spring.mail.password=test",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        // JWT
        "jwt.secret=test-secret-key-min-32-chars-for-hs512-1234567890",
        "jwt.access-expiration-ms=3600000",
        "jwt.refresh-expiration-ms=604800000",
        // Отключить автоконфигурацию Spring Security для теста
        "spring.security.user.name=test",
        "spring.security.user.password=test"
})
@DisplayName("UserService — Smoke Tests")
class UserServiceSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Сервис стартует успешно")
    void contextLoads() {
        // Тест проходит если Spring Context загрузился без ошибок
    }

    @Test
    @DisplayName("GET /actuator/health → 200 OK")
    void healthEndpoint_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register с невалидным телом → 400 Bad Request")
    void register_withInvalidBody_returns400() throws Exception {
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("{}")
        ).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/users/me без X-User-Id → 400 или 401")
    void getProfile_withoutUserId_returns4xx() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().is4xxClientError());
    }
}
