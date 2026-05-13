package kz.noisemap.recordingservice.ml;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.time.Duration;

/**
 * Клиент для ML-сервиса одногруппника (https://github.com/nodirbekUmarov04/thesis-classification-microservice).
 *
 * ML-сервис — это синхронный FastAPI:
 *   POST http://ml-service:8000/predict
 *   Body: multipart/form-data, file=@audio.wav
 *   Response: {"label": "transport", "confidence": 0.977, ...}
 *
 * Поддерживаемые classes (от одногруппника):
 *   transport, human, alert, building_noise, animals, others
 *
 * Маппинг в наши classes делается в MlBridgeListener:
 *   transport      → traffic
 *   human          → voices
 *   alert          → siren
 *   building_noise → construction
 *   animals        → nature
 *   others         → other
 *
 * Fail-safe: при ошибке возвращает null, обработка вверх по стеку.
 */
@Slf4j
@Component
public class MlClient {

    private final WebClient webClient;
    private final Duration timeout;

    public MlClient(
            @Value("${services.ml-service.url:http://ml-service:8000}") String mlServiceUrl,
            @Value("${services.ml-service.timeout-seconds:30}") int timeoutSeconds) {
        this.webClient = WebClient.builder()
                .baseUrl(mlServiceUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024)) // 20MB для аудио
                .build();
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        log.info("ML client initialized: url={}, timeout={}s", mlServiceUrl, timeoutSeconds);
    }

    /**
     * Классифицировать аудиофайл через ML-сервис.
     * Блокирующий вызов — рассчитан на использование внутри @Async или RabbitListener.
     *
     * @param audioFile WAV или MP3 файл
     * @return результат классификации или null при ошибке
     */
    public MlClassificationResponse classify(File audioFile) {
        if (audioFile == null || !audioFile.exists()) {
            log.error("ML classify: audio file does not exist: {}",
                    audioFile != null ? audioFile.getAbsolutePath() : "null");
            return null;
        }

        try {
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new FileSystemResource(audioFile))
                    .header("Content-Disposition", "form-data; name=file; filename=" + audioFile.getName());

            MlClassificationResponse result = webClient.post()
                    .uri("/predict")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(MlClassificationResponse.class)
                    .timeout(timeout)
                    .block();

            if (result == null) {
                log.warn("ML classify: empty response for {}", audioFile.getName());
                return null;
            }

            log.info("ML classify: file={}, label={}, confidence={}, dba={}",
                    audioFile.getName(), result.getLabel(),
                    result.getConfidence(), result.getNoiseLevelDba());

            return result;
        } catch (Exception e) {
            log.error("ML classify failed for {}: {}", audioFile.getName(), e.getMessage());
            return null;
        }
    }
}
