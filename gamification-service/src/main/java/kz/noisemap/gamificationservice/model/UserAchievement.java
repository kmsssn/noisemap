package kz.noisemap.gamificationservice.model;

import jakarta.persistence.*;
import lombok.*;

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
    private String achievementTitle;

    private String description;

    private Integer pointsAwarded;

    @Column(nullable = false)
    private Instant unlockedAt;
}
