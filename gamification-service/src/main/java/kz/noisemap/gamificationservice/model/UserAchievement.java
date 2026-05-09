package kz.noisemap.gamificationservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_achievements",
       uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "achievementCode"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String achievementCode;

    @Column(nullable = false)
    private Instant unlockedAt;

    /**
     * Связь с определением ачивки.
     * Используем FetchType.LAZY — не грузим определение если не нужно.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievementCode", insertable = false, updatable = false)
    private AchievementDefinition definition;
}
