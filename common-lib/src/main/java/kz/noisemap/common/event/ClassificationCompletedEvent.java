package kz.noisemap.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Публикуется ML Classification Service после обработки.
 * Потребители: Mapping, Statistics, Gamification, Notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationCompletedEvent implements Serializable {
    private String recordingId;
    private UUID userId;
    private Double latitude;
    private Double longitude;
    private Double noiseLevelDba;        // уровень шума в дБА (с учётом калибровки)
    private String noiseClass;           // "traffic", "construction", "voices", "siren", "music", "nature", "industrial", "other"
    private Double confidenceScore;      // 0.0 - 1.0
    private Instant recordedAt;
    private Instant classifiedAt;
}
