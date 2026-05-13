package kz.noisemap.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class DeviceCalibrationDto {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private UUID id;
        private String manufacturer;
        private String model;
        private Double calibrationOffsetDb;
        private String source;
        private Boolean verified;
        private Integer sampleCount;
        private Instant createdAt;
        private Instant updatedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CalibrationResult {
        private String model;
        private Double calibrationOffsetDb;
        private Boolean verified;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank
        @Size(max = 50)
        private String manufacturer;
        @NotBlank
        @Size(max = 100)
        private String model;
        @NotNull
        private Double calibrationOffsetDb;
        private String source;
        private Boolean verified;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UpdateRequest {
        @Size(max = 50)
        private String manufacturer;
        private Double calibrationOffsetDb;
        private String source;
        private Boolean verified;
    }
}