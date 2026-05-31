package kz.noisemap.statisticsservice.client;

import kz.noisemap.statisticsservice.dto.PredictionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;


@Slf4j
@Component
@RequiredArgsConstructor
public class PredictionClient {

    private final RestClient predictionRestClient;

    /**
     * @param time может быть null -> prediction подставит текущее время Алматы
     */
    public PredictionDto.PointPrediction predictPoint(double lat, double lon, String time) {
        try {
            String uri = UriComponentsBuilder.fromPath("/predict")
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .queryParamIfPresent("time",
                            (time == null || time.isBlank())
                                    ? java.util.Optional.empty()
                                    : java.util.Optional.of(time))
                    .build()
                    .toUriString();

            PredictionDto.PointPrediction result = predictionRestClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(PredictionDto.PointPrediction.class);

            if (result == null) {
                log.warn("Prediction: empty response for lat={}, lon={}", lat, lon);
            }
            return result;
        } catch (Exception e) {
            log.error("Prediction call failed for lat={}, lon={}: {}", lat, lon, e.getMessage());
            return null;
        }
    }
}