package kz.noisemap.statisticsservice.service;

import kz.noisemap.statisticsservice.client.PredictionClient;
import kz.noisemap.statisticsservice.dto.PredictionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PredictionService {

    private final PredictionClient predictionClient;

    /** Точка -> типизированный ответ. */
    public PredictionDto.PredictionResponse getPrediction(double lat, double lon, String time) {
        PredictionDto.PointPrediction raw = predictionClient.predictPoint(lat, lon, time);
        if (raw == null) {
            return PredictionDto.PredictionResponse.builder()
                    .latitude(lat).longitude(lon).build();
        }
        return PredictionDto.PredictionResponse.builder()
                .latitude(raw.getLatitude() != null ? raw.getLatitude() : lat)
                .longitude(raw.getLongitude() != null ? raw.getLongitude() : lon)
                .predictedNoiseDba(raw.getNoiseLevelDba())
                .predictedNoiseClass(raw.getNoiseClass())
                .time(raw.getTime())
                .dayOfWeek(raw.getDayOfWeek())
                .build();
    }

    /** Область -> сырой GeoJSON (или null, если prediction недоступен). */
    public String getBboxPrediction(String bbox, String time) {
        return predictionClient.predictBbox(bbox, time);
    }
}