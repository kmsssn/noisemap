package kz.noisemap.gamificationservice.event;

import kz.noisemap.common.event.ClassificationCompletedEvent;
import kz.noisemap.common.event.RabbitConstants;
import kz.noisemap.common.event.RecordingCreatedEvent;
import kz.noisemap.gamificationservice.service.GamificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GamificationEventListener {

    private final GamificationService gamificationService;

    /**
     * Начислить очки за новую запись (до ML классификации).
     */
    @RabbitListener(queues = RabbitConstants.Q_GAMIFICATION_RECORDING)
    public void handleRecordingCreated(RecordingCreatedEvent event) {
        log.info("Gamification: new recording from user {}", event.getUserId());
        try {
            gamificationService.handleNewRecording(event);
        } catch (Exception e) {
            log.error("Failed to process gamification for recording: {}", event.getRecordingId(), e);
        }
    }

    /**
     * Проверить ачивки по результату классификации (тишина, громкость).
     */
    @RabbitListener(queues = RabbitConstants.Q_GAMIFICATION_ACHIEVEMENT)
    public void handleClassificationCompleted(ClassificationCompletedEvent event) {
        log.info("Gamification: classification result for user {}, dBA={}",
                event.getUserId(), event.getNoiseLevelDba());
        try {
            gamificationService.handleClassificationResult(event);
        } catch (Exception e) {
            log.error("Failed to check achievements for recording: {}", event.getRecordingId(), e);
        }
    }
}
