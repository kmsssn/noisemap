package kz.noisemap.recordingservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;


@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceCalibrationClient {

    private final WebClient userServiceWebClient;

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    /**
     * Запрашивает калибровку для модели устройства.
     */
    public Double getCalibrationOffset(String deviceModel) {
        if (deviceModel == null || deviceModel.isBlank()) {
            return null;
        }

        try {
            CalibrationDto result = userServiceWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/devices/calibration")
                            .queryParam("model", deviceModel)
                            .build())
                    .retrieve()
                    .bodyToMono(CalibrationDto.class)
                    .timeout(TIMEOUT)
                    .block();

            if (result == null || result.getCalibrationOffsetDb() == null) {
                log.warn("Empty calibration response for device: {} -> null (ML applies default)", deviceModel);
                return null;
            }

            log.debug("Calibration for {}: offset={} dB, verified={}",
                    deviceModel, result.getCalibrationOffsetDb(), result.getVerified());

            return result.getCalibrationOffsetDb();

        } catch (Exception e) {
            log.error("Failed to get calibration for device '{}': {}. Returning null (ML applies default)",
                    deviceModel, e.getMessage());
            return null;
        }
    }
}