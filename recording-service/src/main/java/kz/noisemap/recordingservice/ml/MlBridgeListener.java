package kz.noisemap.recordingservice.ml;

import kz.noisemap.common.event.ClassificationCompletedEvent;
import kz.noisemap.common.event.RabbitConstants;
import kz.noisemap.common.event.RecordingCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Instant;

/**
 * Bridge между RabbitMQ архитектурой NoiseMap и HTTP-based ML-сервисом.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MlBridgeListener {

    private final MlClient mlClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${storage.audio.path:/data/audio}")
    private String audioBasePath;

    @Value("${app.ml.default-dba-placeholder:55.0}")
    private double defaultDbaPlaceholder;

    private static final java.util.Set<String> KNOWN_CLASSES = java.util.Set.of(
            "transport", "human", "alert", "building_noise", "animals", "others"
    );

    @RabbitListener(queues = RabbitConstants.Q_ML_CLASSIFICATION)
    public void handleRecordingCreated(RecordingCreatedEvent event) {
        log.info("ML bridge: received recording {} from user {}",
                event.getRecordingId(), event.getUserId());

        File audioFile = resolveAudioFile(event.getAudioFileUrl());
        if (audioFile == null || !audioFile.exists()) {
            log.error("ML bridge: audio file not found for recording {}: {}",
                    event.getRecordingId(), event.getAudioFileUrl());
            return;
        }

        MlClassificationResponse mlResponse = mlClient.classify(audioFile);
        if (mlResponse == null || mlResponse.getLabel() == null) {
            log.error("ML bridge: classification failed for recording {}", event.getRecordingId());
            return;
        }

        String noiseClass = mlResponse.getLabel();
        if (!KNOWN_CLASSES.contains(noiseClass)) {
            log.warn("ML bridge: unknown class '{}' from ML — sending anyway. " +
                    "Consider updating KNOWN_CLASSES.", noiseClass);
        }

        double finalDba = computeFinalDba(mlResponse, event.getCalibrationOffset());

        ClassificationCompletedEvent completedEvent = ClassificationCompletedEvent.builder()
                .recordingId(event.getRecordingId())
                .userId(event.getUserId())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .noiseLevelDba(finalDba)
                .noiseClass(noiseClass)
                .confidenceScore(mlResponse.getConfidence())
                .recordedAt(event.getRecordedAt())
                .classifiedAt(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitConstants.EXCHANGE,
                RabbitConstants.RK_CLASSIFICATION_COMPLETED,
                completedEvent
        );

        log.info("ML bridge: published classification.completed for {}: class={}, dba={}, confidence={}",
                event.getRecordingId(), noiseClass, finalDba, mlResponse.getConfidence());
    }

    private File resolveAudioFile(String audioFileUrl) {
        if (audioFileUrl == null || audioFileUrl.isBlank()) {
            return null;
        }
        if (audioFileUrl.startsWith("/")) {
            return new File(audioFileUrl);
        }
        return new File(audioBasePath, audioFileUrl);
    }

    /**
     * Вычисляет финальный dBA с учётом калибровки устройства.
     */
    private double computeFinalDba(MlClassificationResponse mlResponse, Double calibrationOffset) {
        double rawDba;
        if (mlResponse.getNoiseLevelDba() != null) {
            rawDba = mlResponse.getNoiseLevelDba();
        } else {
            log.warn("ML bridge: noise_level_dba is null — using placeholder {}. " +
                    "Ask ML team to add RMS-based dBA to /predict response.",
                    defaultDbaPlaceholder);
            rawDba = defaultDbaPlaceholder;
        }

        double offset = calibrationOffset != null ? calibrationOffset : 0.0;
        return rawDba + offset;
    }
}
