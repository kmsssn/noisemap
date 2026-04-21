package kz.noisemap.mappingservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Агрегированные данные шума для одной ячейки гексагональной сетки.
 * Каждая ячейка хранит средний уровень шума, количество измерений,
 * распределение по классам шума.
 */
@Document(collection = "noise_tiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoiseTile {

    @Id
    private String id;

    /**
     * Hex grid координаты — идентификатор ячейки.
     * Формат: "zoom_x_y" (например "14_12345_6789")
     */
    private String tileKey;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private double[] center; // [longitude, latitude] — центр ячейки

    private Double avgNoiseLevelDba;   // Leq — средний уровень шума
    private Double minNoiseLevelDba;
    private Double maxNoiseLevelDba;

    private Integer measurementCount;  // сколько измерений в этой ячейке

    /**
     * Распределение по классам: {"traffic": 45, "construction": 12, "voices": 8}
     */
    private Map<String, Integer> noiseClassDistribution;

    /**
     * Доминирующий класс шума в этой ячейке.
     */
    private String dominantNoiseClass;

    private Instant lastUpdated;
}
