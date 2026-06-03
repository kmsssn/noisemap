package kz.noisemap.moderationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class ModerationDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueueItem {
        private String id;
        private String recordingId;
        private UUID userId;
        private String reason;
        private String details;
        private String status;
        private Instant flaggedAt;
        private UUID reviewedBy;
        private String reviewComment;
        private Instant reviewedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewRequest {
        private String decision;
        private String comment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueueStats {
        private Long pending;
        private Long approvedToday;
        private Long rejectedToday;
    }
}
