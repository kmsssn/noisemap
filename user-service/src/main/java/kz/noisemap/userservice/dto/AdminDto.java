package kz.noisemap.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

public class AdminDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private UUID id;
        private String email;
        private String displayName;
        private String role;
        private String language;
        private Boolean active;
        private String blockReason;
        private Instant blockedAt;
        private Instant blockedUntil;
        private Instant createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeRoleRequest {
        private String role;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SetActiveRequest {
        private Boolean active;
        private String reason;
        private Integer durationHours;
    }
}
