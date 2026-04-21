package kz.noisemap.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Публикуется Moderation Service при обнаружении аномалии.
 * Потребитель: Notification Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingFlaggedEvent implements Serializable {
    private String recordingId;
    private UUID userId;
    private String reason;             // "anomaly_detected", "spam_pattern", "unrealistic_value"
    private String details;
    private Instant flaggedAt;
}
