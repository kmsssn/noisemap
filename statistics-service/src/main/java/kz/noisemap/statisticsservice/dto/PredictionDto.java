package kz.noisemap.statisticsservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class PredictionDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PointPrediction {
        @JsonProperty("noise_level_dba")
        private Double noiseLevelDba;

        @JsonProperty("noise_class")
        private String noiseClass;

        private Double latitude;
        private Double longitude;
        private String time;

        @JsonProperty("day_of_week")
        private String dayOfWeek;
    }


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictionResponse {
        private Double latitude;
        private Double longitude;
        private Double predictedNoiseDba;
        private String predictedNoiseClass;
        private String time;
        private String dayOfWeek;
    }
}