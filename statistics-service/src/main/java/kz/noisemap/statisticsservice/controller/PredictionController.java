package kz.noisemap.statisticsservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.noisemap.statisticsservice.dto.PredictionDto;
import kz.noisemap.statisticsservice.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stats/predict")
@RequiredArgsConstructor
@Tag(name = "Прогноз шума", description = "Прогноз уровня и класса шума (ML prediction)")
public class PredictionController {

    private final PredictionService predictionService;

    /**
     * Прогноз шума. Два режима:
     *   - точка:   ?lat=..&lon=..[&time=..]   -> JSON с прогнозом для точки
     *   - область: ?bbox=min_lon,min_lat,max_lon,max_lat[&time=..] -> GeoJSON FeatureCollection
     * Если задан bbox, он имеет приоритет; lat/lon тогда не требуются.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Прогноз шума (точка или область)",
            description = "Передай либо bbox (сетка для тепловой карты, вернётся GeoJSON), "
                    + "либо lat+lon (одна точка). time опционально (ISO 8601). Публичный endpoint.")
    public ResponseEntity<?> predict(
            @Parameter(description = "bbox: min_lon,min_lat,max_lon,max_lat")
            @RequestParam(required = false) String bbox,
            @Parameter(description = "Широта (для точки)", example = "43.238949")
            @RequestParam(required = false) Double lat,
            @Parameter(description = "Долгота (для точки)", example = "76.889709")
            @RequestParam(required = false) Double lon,
            @Parameter(description = "Время ISO 8601", example = "2026-06-01T14:00:00")
            @RequestParam(required = false) String time) {

        // Режим области (приоритет)
        if (bbox != null && !bbox.isBlank()) {
            String geoJson = predictionService.getBboxPrediction(bbox, time);
            if (geoJson == null) {
                return ResponseEntity.status(502)
                        .body("{\"error\":\"prediction service unavailable\"}");
            }
            return ResponseEntity.ok(geoJson);
        }

        // Режим точки
        if (lat == null || lon == null) {
            return ResponseEntity.badRequest()
                    .body("{\"error\":\"provide either bbox, or both lat and lon\"}");
        }
        PredictionDto.PredictionResponse resp = predictionService.getPrediction(lat, lon, time);
        return ResponseEntity.ok(resp);
    }
}