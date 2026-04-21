package kz.noisemap.moderationservice.service;

import kz.noisemap.common.event.RabbitConstants;
import kz.noisemap.common.event.RecordingCreatedEvent;
import kz.noisemap.common.event.RecordingFlaggedEvent;
import kz.noisemap.moderationservice.dto.ModerationDto;
import kz.noisemap.moderationservice.model.ModerationRecord;
import kz.noisemap.moderationservice.model.ModerationStatus;
import kz.noisemap.moderationservice.repository.ModerationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ModerationRepository moderationRepository;
    private final RabbitTemplate rabbitTemplate;

    // Пороги для автоматического флагирования
    private static final int MAX_RECORDINGS_PER_MINUTE = 5;
    private static final double MIN_VALID_DBA = 10.0;
    private static final double MAX_VALID_DBA = 140.0;
    private static final double MIN_VALID_LAT = 40.0;  // Примерные границы Алматы
    private static final double MAX_VALID_LAT = 44.0;
    private static final double MIN_VALID_LNG = 76.0;
    private static final double MAX_VALID_LNG = 78.0;

    /**
     * Автоматическая проверка новой записи при поступлении recording.created.
     */
    public void checkRecording(RecordingCreatedEvent event) {
        String reason = null;
        String details = null;

        // Проверка координат — в пределах Алматы
        if (event.getLatitude() < MIN_VALID_LAT || event.getLatitude() > MAX_VALID_LAT
                || event.getLongitude() < MIN_VALID_LNG || event.getLongitude() > MAX_VALID_LNG) {
            reason = "out_of_bounds";
            details = String.format("Coordinates [%.4f, %.4f] outside Almaty area",
                    event.getLatitude(), event.getLongitude());
        }

        // Проверка частоты записей — антиспам
        long recentCount = moderationRepository.countByUserId(event.getUserId());
        // Упрощённая проверка: если у пользователя слишком много флагов — подозрительный
        if (recentCount > 10) {
            reason = "spam_pattern";
            details = "User has " + recentCount + " flagged records";
        }

        if (reason != null) {
            flagRecording(event.getRecordingId(), event.getUserId(), reason, details);
        }
    }

    /**
     * Пометить запись как подозрительную.
     */
    public void flagRecording(String recordingId, UUID userId, String reason, String details) {
        ModerationRecord record = ModerationRecord.builder()
                .recordingId(recordingId)
                .userId(userId)
                .reason(reason)
                .details(details)
                .status(ModerationStatus.PENDING)
                .flaggedAt(Instant.now())
                .build();

        moderationRepository.save(record);

        // Публикуем событие → Notification Service
        RecordingFlaggedEvent event = RecordingFlaggedEvent.builder()
                .recordingId(recordingId)
                .userId(userId)
                .reason(reason)
                .details(details)
                .flaggedAt(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitConstants.EXCHANGE,
                RabbitConstants.RK_RECORDING_FLAGGED,
                event
        );

        log.info("Recording {} flagged: reason={}", recordingId, reason);
    }

    /**
     * Очередь на модерацию — для модераторов.
     */
    public Page<ModerationDto.QueueItem> getPendingQueue(Pageable pageable) {
        return moderationRepository.findByStatus(ModerationStatus.PENDING, pageable)
                .map(this::toQueueItem);
    }

    /**
     * Модератор принимает решение по записи.
     */
    public void reviewRecord(String recordId, UUID moderatorId, ModerationDto.ReviewRequest request) {
        ModerationRecord record = moderationRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));

        if ("approve".equalsIgnoreCase(request.getDecision())) {
            record.setStatus(ModerationStatus.APPROVED);
        } else if ("reject".equalsIgnoreCase(request.getDecision())) {
            record.setStatus(ModerationStatus.REJECTED);
        } else {
            throw new IllegalArgumentException("Invalid decision: " + request.getDecision());
        }

        record.setReviewedBy(moderatorId);
        record.setReviewComment(request.getComment());
        record.setReviewedAt(Instant.now());

        moderationRepository.save(record);
        log.info("Moderation review: recordId={}, decision={}, moderator={}",
                recordId, request.getDecision(), moderatorId);
    }

    public ModerationDto.QueueStats getQueueStats() {
        return ModerationDto.QueueStats.builder()
                .pending(moderationRepository.countByStatus(ModerationStatus.PENDING))
                .build();
    }

    private ModerationDto.QueueItem toQueueItem(ModerationRecord record) {
        return ModerationDto.QueueItem.builder()
                .id(record.getId())
                .recordingId(record.getRecordingId())
                .userId(record.getUserId())
                .reason(record.getReason())
                .details(record.getDetails())
                .status(record.getStatus().name())
                .flaggedAt(record.getFlaggedAt())
                .build();
    }
}
