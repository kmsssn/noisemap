package kz.noisemap.statisticsservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.noisemap.statisticsservice.dto.PredictionDto;
import kz.noisemap.statisticsservice.service.PredictionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stats/predict")
@RequiredArgsConstructor
@Tag(name = "Прогноз шума", description = "Прогноз уровня и класса шума по координате и времени (ML prediction)")
public class PredictionController {

    private final PredictionService predictionService;

    @GetMapping
    @Operation(summary = "Прогноз шума в точке",
            description = "Возвращает прогнозируемый уровень (дБА) и класс шума для заданной координаты "
                    + "и времени. time опционально (ISO 8601); если не задано — используется текущее время Алматы. "
                    + "Публичный endpoint.")
    public ResponseEntity<PredictionDto.PredictionResponse> predict(
            @Parameter(description = "Широта", example = "43.238949")
            @RequestParam double lat,
            @Parameter(description = "Долгота", example = "76.889709")
            @RequestParam double lon,
            @Parameter(description = "Время ISO 8601, напр. 2026-06-01T14:00:00", example = "2026-06-01T14:00:00")
            @RequestParam(required = false) String time) {
        return ResponseEntity.ok(predictionService.getPrediction(lat, lon, time));
    }
}