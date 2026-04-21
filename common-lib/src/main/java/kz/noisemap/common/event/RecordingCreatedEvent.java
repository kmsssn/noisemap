package kz.noisemap.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Публикуется Recording Service после сохранения аудио.
 * Потребители: ML Classification, Moderation, Gamification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingCreatedEvent implements Serializable {
    private String recordingId;
    private UUID userId;
    private String audioFileUrl;
    private Double latitude;
    private Double longitude;
    private String deviceModel;
    private Double calibrationOffset;
    private Instant recordedAt;
    private Instant publishedAt;
}
