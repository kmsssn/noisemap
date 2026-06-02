package kz.noisemap.commentservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class CommentDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull
        @DecimalMin("-90") @DecimalMax("90")
        private Double latitude;

        @NotNull
        @DecimalMin("-180") @DecimalMax("180")
        private Double longitude;

        @NotBlank
        @Size(min = 3, max = 500, message = "Comment must be between 3 and 500 characters")
        private String text;

        private String noiseClass;
        private Double noiseLevelDba;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private String id;
        private UUID userId;
        private String displayName;
        private Double latitude;
        private Double longitude;
        private String text;
        private String noiseClass;
        private Double noiseLevelDba;
        private Boolean hidden;
        private Instant createdAt;
    }
}
