package kz.noisemap.moderationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "moderation_queue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationRecord {

    @Id
    private String id;

    @Indexed
    private String recordingId;

    @Indexed
    private UUID userId;

    private String reason;
    private String details;

    @Builder.Default
    private ModerationStatus status = ModerationStatus.PENDING;

    private UUID reviewedBy;
    private String reviewedByName;
    private String reviewComment;

    private Instant flaggedAt;
    private Instant reviewedAt;
}
