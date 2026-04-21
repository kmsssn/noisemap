package kz.noisemap.statisticsservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.noisemap.statisticsservice.dto.StatsDto;
import kz.noisemap.statisticsservice.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Tag(name = "Статистика", description = "Аналитика шумового загрязнения по городу и персональная статистика пользователя")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/city")
    @Operation(summary = "Общая статистика по городу",
               description = "Средний, минимальный и максимальный уровень шума, "
                       + "распределение по классам, почасовые средние, количество измерений и контрибьюторов. "
                       + "Публичный endpoint. Кэшируется в Redis (10 мин).")
    public ResponseEntity<StatsDto.CityStatsResponse> getCityStats() {
        return ResponseEntity.ok(statisticsService.getCityStats());
    }

    @GetMapping("/me")
    @Operation(summary = "Персональная статистика",
               description = "Статистика шумового воздействия на пользователя: "
                       + "средний уровень шума, распределение по классам, "
                       + "рекомендации на основе норм ВОЗ (<55 дБА — норма, 55-70 — умеренно, "
                       + "70-85 — повышенный, >85 — опасный)")
    public ResponseEntity<StatsDto.UserStatsResponse> getMyStats(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(statisticsService.getUserStats(userId));
    }
}
