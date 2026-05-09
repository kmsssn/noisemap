package kz.noisemap.gamificationservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Определение ачивки — хранится в БД.
 * Добавить новую ачивку = INSERT в таблицу, без редеплоя.
 *
 * trigger_type определяет условие:
 *   recording_count   — trigger_value = минимальное кол-во записей ("1", "10", "50")
 *   noise_level_below — trigger_value = максимальный дБА ("<40")
 *   noise_level_above — trigger_value = минимальный дБА (">85")
 *   time_of_day       — trigger_value = "night" (23-5) или "morning" (5-7)
 *   streak_days       — trigger_value = минимальный стрик ("7", "30")
 */
@Entity
@Table(name = "achievement_definitions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDefinition {

    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "points", nullable = false)
    private Integer points;        // очки за ачивку

    @Column(name = "trigger_type", nullable = false)
    private String triggerType;    // тип условия

    @Column(name = "trigger_value")
    private String triggerValue;   // значение условия

    @Column(name = "icon_url")
    private String iconUrl;        // ссылка на иконку: "/icons/achievements/first_recording.svg"

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt;
}