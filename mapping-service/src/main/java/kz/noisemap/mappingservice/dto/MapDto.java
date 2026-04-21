package kz.noisemap.mappingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class MapDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TileResponse {
        private String tileKey;
        private Double latitude;
        private Double longitude;
        private Double avgNoiseLevelDba;
        private Integer measurementCount;
        private String dominantNoiseClass;
        private Map<String, Integer> noiseClassDistribution;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapResponse {
        private List<TileResponse> tiles;
        private Double overallAvgDba;
        private Integer totalMeasurements;
        private BoundingBox boundingBox;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoundingBox {
        private Double minLat;
        private Double minLng;
        private Double maxLat;
        private Double maxLng;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapRequest {
        private Double minLat;
        private Double minLng;
        private Double maxLat;
        private Double maxLng;
        private String noiseClassFilter;  // null = все классы
        private String timeFilter;         // "morning", "afternoon", "evening", "night"
    }
}
