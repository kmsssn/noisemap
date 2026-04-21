package kz.noisemap.mappingservice.event;

import kz.noisemap.common.event.ClassificationCompletedEvent;
import kz.noisemap.common.event.RabbitConstants;
import kz.noisemap.mappingservice.service.MappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClassificationEventListener {

    private final MappingService mappingService;

    @RabbitListener(queues = RabbitConstants.Q_MAPPING_UPDATE)
    public void handleClassificationCompleted(ClassificationCompletedEvent event) {
        log.info("Mapping: received classification for recording {} at [{}, {}]",
                event.getRecordingId(), event.getLatitude(), event.getLongitude());

        try {
            mappingService.updateTileWithMeasurement(event);
        } catch (Exception e) {
            log.error("Failed to update tile for recording: {}", event.getRecordingId(), e);
        }
    }
}
