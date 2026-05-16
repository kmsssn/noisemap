package kz.noisemap.statisticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

public class StatsDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CityStatsResponse {
        private Double avgNoiseLevelDba;
        private Double maxNoiseLevelDba;
        private Double minNoiseLevelDba;
        private Long totalMeasurements;
        private Long totalContributors;
        private Map<String, Long> measurementsByNoiseClass;
        private List<HourlyAverage> hourlyAverages;
        private String noisestArea;   // район с наибольшим шумом
        private String quietestArea;  // район с наименьшим шумом
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatsResponse {
        private Long totalRecordings;
        private Double avgExposureDba;
        private Double maxExposureDba;
        private Map<String, Long> recordingsByNoiseClass;
        private List<HourlyAverage> personalHourlyAverages;
        private String recommendationKey;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyAverage {
        private Integer hour;
        private Double avgDba;
        private Long measurementCount;
    }
}
