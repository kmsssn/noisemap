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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ModerationRepository moderationRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final double MIN_VALID_LAT = 40.0;
    private static final double MAX_VALID_LAT = 44.0;
    private static final double MIN_VALID_LNG = 76.0;
    private static final double MAX_VALID_LNG = 78.0;

    private static final int SPAM_FLAG_THRESHOLD = 5;
    private static final int SPAM_WINDOW_HOURS = 1;


    public void checkRecording(RecordingCreatedEvent event) {
        if (event.getLatitude() < MIN_VALID_LAT || event.getLatitude() > MAX_VALID_LAT
                || event.getLongitude() < MIN_VALID_LNG || event.getLongitude() > MAX_VALID_LNG) {
            String details = String.format("Coordinates [%.4f, %.4f] outside Almaty area",
                    event.getLatitude(), event.getLongitude());
            flagRecording(event.getRecordingId(), event.getUserId(), "out_of_bounds", details);
            return;
        }

        Instant windowStart = Instant.now().minus(SPAM_WINDOW_HOURS, ChronoUnit.HOURS);
        long recentFlags = moderationRepository.countByUserIdAndFlaggedAtAfter(
                event.getUserId(), windowStart);

        if (recentFlags >= SPAM_FLAG_THRESHOLD) {
            String details = String.format(
                    "User has %d flagged records in the last %d hour(s)", recentFlags, SPAM_WINDOW_HOURS);
            flagRecording(event.getRecordingId(), event.getUserId(), "spam_pattern", details);
        }
    }


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

    public Page<ModerationDto.QueueItem> getPendingQueue(Pageable pageable) {
        return moderationRepository.findByStatus(ModerationStatus.PENDING, pageable)
                .map(this::toQueueItem);
    }

    /**
     * Очередь/история по статусу. status == null или "ALL" -> все записи (история).
     */
    public Page<ModerationDto.QueueItem> getQueue(String status, Pageable pageable) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return moderationRepository.findAll(pageable).map(this::toQueueItem);
        }
        ModerationStatus st;
        try {
            st = ModerationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status. Allowed: PENDING, APPROVED, REJECTED, ALL");
        }
        return moderationRepository.findByStatus(st, pageable).map(this::toQueueItem);
    }

    public void reviewRecord(String id, UUID reviewerId, ModerationDto.ReviewRequest request) {
        ModerationRecord record = moderationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Moderation record not found: " + id));

        if (!"approve".equals(request.getDecision()) && !"reject".equals(request.getDecision())) {
            throw new IllegalArgumentException("Decision must be 'approve' or 'reject'");
        }

        record.setStatus("approve".equals(request.getDecision())
                ? ModerationStatus.APPROVED
                : ModerationStatus.REJECTED);
        record.setReviewedBy(reviewerId);
        record.setReviewComment(request.getComment());
        record.setReviewedAt(Instant.now());

        moderationRepository.save(record);
        log.info("Recording {} moderation decision: {} by {}", id, request.getDecision(), reviewerId);
    }

    public ModerationDto.QueueStats getQueueStats() {
        return ModerationDto.QueueStats.builder()
                .pending(moderationRepository.countByStatus(ModerationStatus.PENDING))
                .approvedToday(moderationRepository.countByStatus(ModerationStatus.APPROVED))
                .rejectedToday(moderationRepository.countByStatus(ModerationStatus.REJECTED))
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
                .reviewedBy(record.getReviewedBy())
                .reviewComment(record.getReviewComment())
                .reviewedAt(record.getReviewedAt())
                .build();
    }
}