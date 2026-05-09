package kz.noisemap.gamificationservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_achievements",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "achievement_code"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "achievement_code", nullable = false)
    private String achievementCode;

    @CreationTimestamp
    @Column(name = "unlocked_at", nullable = false, updatable = false)
    private Instant unlockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_code", insertable = false, updatable = false)
    private AchievementDefinition definition;
}