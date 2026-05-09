package kz.noisemap.gamificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class GamificationDto {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProfileResponse {
        private UUID userId;
        private Integer totalPoints;
        private Integer totalRecordings;
        private Integer level;
        private Integer currentStreak;
        private List<AchievementResponse> achievements;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AchievementResponse {
        private String code;
        private String title;
        private String description;
        private Integer pointsAwarded;
        private String iconUrl;         // URL на иконку
        private Instant unlockedAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AchievementDefinitionResponse {
        private String code;
        private String title;
        private String description;
        private Integer points;
        private String triggerType;
        private String triggerValue;
        private String iconUrl;
        private Boolean active;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LeaderboardEntry {
        private Integer rank;
        private UUID userId;
        private String displayName;
        private Integer totalPoints;
        private Integer totalRecordings;
        private Integer level;
    }
}
