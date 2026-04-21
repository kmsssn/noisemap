package kz.noisemap.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Публикуется Gamification Service при получении ачивки.
 * Потребитель: Notification Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementUnlockedEvent implements Serializable {
    private UUID userId;
    private String achievementCode;
    private String achievementTitle;
    private Integer pointsAwarded;
    private Instant unlockedAt;
}
