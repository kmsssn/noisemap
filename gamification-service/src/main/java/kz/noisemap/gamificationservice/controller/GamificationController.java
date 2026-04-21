package kz.noisemap.gamificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.noisemap.gamificationservice.dto.GamificationDto;
import kz.noisemap.gamificationservice.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
@Tag(name = "Геймификация", description = "Система мотивации: очки, ачивки, уровни, лидерборд. "
        + "Пользователи получают 10 очков за каждую запись + бонусы за ачивки.")
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/me")
    @Operation(summary = "Мой профиль геймификации",
               description = "Возвращает очки, уровень, текущий стрик, список разблокированных ачивок. "
                       + "Ачивки: Первый шаг (1 запись), Активист (10), Исследователь (50), "
                       + "Ночной дозор (запись 23:00-5:00), Тишина и покой (<40 дБА) и др.")
    public ResponseEntity<GamificationDto.ProfileResponse> getMyProfile(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(gamificationService.getProfile(userId));
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Лидерборд",
               description = "Топ пользователей по очкам. Возвращает ранг, очки, количество записей, уровень.")
    public ResponseEntity<List<GamificationDto.LeaderboardEntry>> getLeaderboard(
            @Parameter(description = "Количество позиций в лидерборде", example = "20")
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(gamificationService.getLeaderboard(limit));
    }
}
