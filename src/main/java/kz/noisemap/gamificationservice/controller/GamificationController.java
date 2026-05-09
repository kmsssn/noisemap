package kz.noisemap.gamificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.noisemap.gamificationservice.dto.GamificationDto;
import kz.noisemap.gamificationservice.model.AchievementDefinition;
import kz.noisemap.gamificationservice.service.AchievementCacheService;
import kz.noisemap.gamificationservice.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
@Tag(name = "Геймификация", description = "Очки, ачивки из БД, лидерборд с именами")
public class GamificationController {

    private final GamificationService gamificationService;
    private final AchievementCacheService cacheService;

    @GetMapping("/me")
    @Operation(summary = "Мой профиль геймификации",
               description = "Очки, уровень, стрик, разблокированные ачивки с иконками. "
                       + "Ачивки загружаются JOIN FETCH — один запрос в БД.")
    public ResponseEntity<GamificationDto.ProfileResponse> getMyProfile(
            @Parameter(description = "UUID пользователя") @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(gamificationService.getProfile(userId));
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Лидерборд", description = "Топ пользователей по очкам с displayName")
    public ResponseEntity<List<GamificationDto.LeaderboardEntry>> getLeaderboard(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(gamificationService.getLeaderboard(limit));
    }

    @GetMapping("/achievements")
    @Operation(summary = "Каталог ачивок",
               description = "Все доступные ачивки с иконками, условиями и очками. "
                       + "Публичный endpoint. Данные из кэша — быстрый ответ.")
    public ResponseEntity<List<GamificationDto.AchievementDefinitionResponse>> getAllAchievements() {
        return ResponseEntity.ok(gamificationService.getAllDefinitions());
    }

    // ============================================================
    // Admin endpoints — управление ачивками без редеплоя
    // ============================================================

    @PostMapping("/achievements")
    @Operation(summary = "Создать ачивку (Admin)",
               description = "Добавить новую ачивку без изменения кода. "
                       + "trigger_type: recording_count | noise_level_below | noise_level_above | time_of_day | streak_days. "
                       + "После создания кэш инвалидируется автоматически.")
    public ResponseEntity<AchievementDefinition> createAchievement(
            @Parameter(description = "Роль") @RequestHeader("X-User-Role") String role,
            @RequestBody AchievementDefinition definition) {
        validateAdmin(role);
        definition.setCreatedAt(Instant.now());
        if (definition.getActive() == null) definition.setActive(true);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cacheService.save(definition));
    }

    @PutMapping("/achievements/{code}")
    @Operation(summary = "Обновить ачивку (Admin)",
               description = "Изменить иконку, описание, очки без редеплоя. Кэш инвалидируется.")
    public ResponseEntity<AchievementDefinition> updateAchievement(
            @Parameter(description = "Роль") @RequestHeader("X-User-Role") String role,
            @PathVariable String code,
            @RequestBody AchievementDefinition definition) {
        validateAdmin(role);
        definition.setCode(code);
        return ResponseEntity.ok(cacheService.save(definition));
    }

    @DeleteMapping("/achievements/{code}")
    @Operation(summary = "Деактивировать ачивку (Admin)",
               description = "Мягкое удаление — ачивка больше не выдаётся новым пользователям, "
                       + "но у тех кто получил — остаётся. Кэш инвалидируется.")
    public ResponseEntity<Void> deactivateAchievement(
            @Parameter(description = "Роль") @RequestHeader("X-User-Role") String role,
            @PathVariable String code) {
        validateAdmin(role);
        cacheService.deactivate(code);
        return ResponseEntity.ok().build();
    }

    private void validateAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new SecurityException("Access denied. Admin role required.");
        }
    }
}
