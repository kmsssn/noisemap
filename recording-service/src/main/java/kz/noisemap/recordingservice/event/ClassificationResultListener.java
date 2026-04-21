package kz.noisemap.recordingservice.event;

import kz.noisemap.common.event.ClassificationCompletedEvent;
import kz.noisemap.recordingservice.service.RecordingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClassificationResultListener {

    private final RecordingService recordingService;

    @RabbitListener(queuesToDeclare = @Queue("recording.classification.result.queue"))
    public void handleClassificationResult(ClassificationCompletedEvent event) {
        log.info("Received classification result for recording: {}", event.getRecordingId());

        try {
            recordingService.updateClassification(
                    event.getRecordingId(),
                    event.getNoiseLevelDba(),
                    event.getNoiseClass(),
                    event.getConfidenceScore()
            );
        } catch (Exception e) {
            log.error("Failed to update classification for recording: {}",
                    event.getRecordingId(), e);
        }
    }
}
