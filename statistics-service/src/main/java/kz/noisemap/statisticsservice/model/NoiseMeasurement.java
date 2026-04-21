package kz.noisemap.statisticsservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Хранит каждое классифицированное измерение для аналитики.
 */
@Document(collection = "noise_measurements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoiseMeasurement {

    @Id
    private String id;

    @Indexed
    private UUID userId;

    private String recordingId;

    private Double latitude;
    private Double longitude;

    private Double noiseLevelDba;
    private String noiseClass;
    private Double confidenceScore;

    @Indexed
    private Instant recordedAt;
    private Instant classifiedAt;

    // Разложенные поля для агрегации
    private Integer hourOfDay;    // 0-23
    private Integer dayOfWeek;    // 1-7 (Mon-Sun)
    private Integer month;        // 1-12
}
