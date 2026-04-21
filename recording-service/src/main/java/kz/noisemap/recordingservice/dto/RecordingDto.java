package kz.noisemap.recordingservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

public class RecordingDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadRequest {
        @NotNull
        @Min(-90) @Max(90)
        private Double latitude;

        @NotNull
        @Min(-180) @Max(180)
        private Double longitude;

        private String deviceModel;
        private Instant recordedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private Double latitude;
        private Double longitude;
        private String status;
        private Double noiseLevelDba;
        private String noiseClass;
        private Double confidenceScore;
        private Instant recordedAt;
        private Instant createdAt;
    }
}
