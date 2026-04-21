package kz.noisemap.mappingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.noisemap.mappingservice.dto.MapDto;
import kz.noisemap.mappingservice.service.MappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/map")
@RequiredArgsConstructor
@Tag(name = "Карта шума", description = "Тепловая карта шумового загрязнения города Алматы. "
        + "Тайлы агрегируются из классифицированных измерений пользователей.")
public class MapController {

    private final MappingService mappingService;

    @GetMapping("/tiles")
    @Operation(summary = "Получить тайлы тепловой карты",
               description = "Возвращает агрегированные данные шума в bounding box. "
                       + "Каждый тайл содержит средний уровень дБА, количество измерений, "
                       + "доминирующий класс шума и распределение по классам. "
                       + "Публичный endpoint — доступен без авторизации. Кэшируется в Redis (5 мин).")
    @ApiResponse(responseCode = "200", description = "Список тайлов с данными шума")
    public ResponseEntity<MapDto.HeatmapResponse> getHeatmap(
            @Parameter(description = "Минимальная широта (юг)", example = "43.20")
            @RequestParam Double minLat,
            @Parameter(description = "Минимальная долгота (запад)", example = "76.85")
            @RequestParam Double minLng,
            @Parameter(description = "Максимальная широта (север)", example = "43.30")
            @RequestParam Double maxLat,
            @Parameter(description = "Максимальная долгота (восток)", example = "76.99")
            @RequestParam Double maxLng,
            @Parameter(description = "Фильтр по классу шума: traffic, construction, voices, siren, music, nature, industrial")
            @RequestParam(required = false) String noiseClass,
            @Parameter(description = "Фильтр по времени суток: morning, afternoon, evening, night")
            @RequestParam(required = false) String timeFilter) {

        MapDto.HeatmapRequest request = MapDto.HeatmapRequest.builder()
                .minLat(minLat).minLng(minLng)
                .maxLat(maxLat).maxLng(maxLng)
                .noiseClassFilter(noiseClass)
                .timeFilter(timeFilter)
                .build();

        return ResponseEntity.ok(mappingService.getHeatmap(request));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Тайлы рядом с точкой",
               description = "Поиск тайлов шума в заданном радиусе от координат. "
                       + "Полезно для оценки шумовой обстановки в конкретном месте.")
    public ResponseEntity<List<MapDto.TileResponse>> getNearby(
            @Parameter(description = "Широта точки", example = "43.238949") @RequestParam Double lat,
            @Parameter(description = "Долгота точки", example = "76.945465") @RequestParam Double lng,
            @Parameter(description = "Радиус поиска в метрах", example = "500") @RequestParam(defaultValue = "500") Double radius) {
        return ResponseEntity.ok(mappingService.getTilesNearPoint(lat, lng, radius));
    }
}
