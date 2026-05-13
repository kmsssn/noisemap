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
     *
     * @param deviceModel модель устройства (например "iPhone 13 Pro")
     * @return offset в dB (по умолчанию 0.0 если устройство неизвестно или сервис недоступен)
     */
    public Double getCalibrationOffset(String deviceModel) {
        if (deviceModel == null || deviceModel.isBlank()) {
            return 0.0;
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
                log.warn("Empty calibration response for device: {}", deviceModel);
                return 0.0;
            }

            log.debug("Calibration for {}: offset={} dB, verified={}",
                    deviceModel, result.getCalibrationOffsetDb(), result.getVerified());

            return result.getCalibrationOffsetDb();

        } catch (Exception e) {
            // Не бросаем дальше — лучше offset=0.0 чем падение всей загрузки
            log.error("Failed to get calibration for device '{}': {}. Falling back to 0.0",
                    deviceModel, e.getMessage());
            return 0.0;
        }
    }
}