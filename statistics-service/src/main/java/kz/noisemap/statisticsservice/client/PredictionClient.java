package kz.noisemap.statisticsservice.client;

import kz.noisemap.statisticsservice.dto.PredictionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PredictionClient {

    private final RestClient predictionRestClient;

    /** Точка. Возвращает типизированный DTO, null при ошибке. */
    public PredictionDto.PointPrediction predictPoint(double lat, double lon, String time) {
        try {
            String uri = UriComponentsBuilder.fromPath("/predict")
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .queryParamIfPresent("time",
                            (time == null || time.isBlank()) ? Optional.empty() : Optional.of(time))
                    .build().toUriString();

            return predictionRestClient.get().uri(uri).retrieve()
                    .body(PredictionDto.PointPrediction.class);
        } catch (Exception e) {
            log.error("Prediction point call failed lat={}, lon={}: {}", lat, lon, e.getMessage());
            return null;
        }
    }

    /**
     * Область (bbox). prediction отдаёт GeoJSON FeatureCollection — проксируем
     * как сырую JSON-строку, не разбирая (структура сетки нужна фронту как есть).
     * @param bbox строка "min_lon,min_lat,max_lon,max_lat"
     */
    public String predictBbox(String bbox, String time) {
        try {
            String uri = UriComponentsBuilder.fromPath("/predict")
                    .queryParam("bbox", bbox)
                    .queryParamIfPresent("time",
                            (time == null || time.isBlank()) ? Optional.empty() : Optional.of(time))
                    .build().toUriString();

            return predictionRestClient.get().uri(uri).retrieve().body(String.class);
        } catch (Exception e) {
            log.error("Prediction bbox call failed bbox={}: {}", bbox, e.getMessage());
            return null;
        }
    }
}
