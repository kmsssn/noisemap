package kz.noisemap.commentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    private String id;

    @Indexed
    private UUID userId;

    private String displayName;    // имя пользователя на момент комментария

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private double[] location;     // [longitude, latitude]

    private String text;           // текст комментария (до 500 символов)

    private String noiseClass;     // опциональный тег: traffic, construction и т.д.

    private Double noiseLevelDba;  // опциональный уровень шума

    @Builder.Default
    private Boolean deleted = false; // мягкое удаление

    @CreatedDate
    private Instant createdAt;
}
