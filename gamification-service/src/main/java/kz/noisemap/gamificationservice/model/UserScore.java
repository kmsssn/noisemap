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
    private UUID userId;

    @Builder.Default
    private Integer totalPoints = 0;

    @Builder.Default
    private Integer totalRecordings = 0;

    @Builder.Default
    private Integer level = 1;

    @Builder.Default
    private Integer currentStreak = 0;  // consecutive days with a recording

    private Instant lastRecordingDate;

    @UpdateTimestamp
    private Instant updatedAt;
}
