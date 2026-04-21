package kz.noisemap.moderationservice.event;

import kz.noisemap.common.event.RabbitConstants;
import kz.noisemap.common.event.RecordingCreatedEvent;
import kz.noisemap.moderationservice.service.ModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModerationEventListener {

    private final ModerationService moderationService;

    @RabbitListener(queues = RabbitConstants.Q_MODERATION_CHECK)
    public void handleRecordingCreated(RecordingCreatedEvent event) {
        log.debug("Moderation: checking recording {} from user {}", event.getRecordingId(), event.getUserId());
        try {
            moderationService.checkRecording(event);
        } catch (Exception e) {
            log.error("Failed moderation check for recording: {}", event.getRecordingId(), e);
        }
    }
}
