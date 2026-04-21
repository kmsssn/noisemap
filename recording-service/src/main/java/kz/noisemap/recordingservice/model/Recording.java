package kz.noisemap.recordingservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "recordings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recording {

    @Id
    private String id;

    private UUID userId;

    private String audioFileUrl;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private double[] location; // [longitude, latitude] — GeoJSON порядок

    private String deviceModel;
    private Double calibrationOffset;

    @Builder.Default
    private RecordingStatus status = RecordingStatus.PENDING;

    // Заполняются после ML классификации
    private Double noiseLevelDba;
    private String noiseClass;
    private Double confidenceScore;

    private Instant recordedAt;

    @CreatedDate
    private Instant createdAt;
}
