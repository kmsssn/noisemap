package kz.noisemap.gamificationservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserScore {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Builder.Default
    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @Builder.Default
    @Column(name = "total_recordings", nullable = false)
    private Integer totalRecordings = 0;

    @Builder.Default
    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Builder.Default
    @Column(name = "current_streak", nullable = false)
    private Integer currentStreak = 0;  // дней подряд с записью

    @Column(name = "last_recording_date")
    private Instant lastRecordingDate;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}