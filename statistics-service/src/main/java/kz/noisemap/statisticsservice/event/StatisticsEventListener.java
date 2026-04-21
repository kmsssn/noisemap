package kz.noisemap.statisticsservice.event;

import kz.noisemap.common.event.ClassificationCompletedEvent;
import kz.noisemap.common.event.RabbitConstants;
import kz.noisemap.statisticsservice.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsEventListener {

    private final StatisticsService statisticsService;

    @RabbitListener(queues = RabbitConstants.Q_STATISTICS_UPDATE)
    public void handleClassificationCompleted(ClassificationCompletedEvent event) {
        log.info("Statistics: received classification for recording {}", event.getRecordingId());
        try {
            statisticsService.saveMeasurement(event);
        } catch (Exception e) {
            log.error("Failed to save measurement for statistics: {}", event.getRecordingId(), e);
        }
    }
}
