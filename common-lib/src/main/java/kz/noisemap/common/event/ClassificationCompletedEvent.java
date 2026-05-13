package kz.noisemap.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationCompletedEvent implements Serializable {
    private String recordingId;
    private UUID userId;
    private Double latitude;
    private Double longitude;
    private Double noiseLevelDba;
    private String noiseClass;           // transport, human, alert, building_noise, animals, others
    private Double confidenceScore;
    private Instant recordedAt;
    private Instant classifiedAt;
}
