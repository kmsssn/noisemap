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
    private String code;           // уникальный код: "first_recording", "quiet_finder"

    @Column(nullable = false)
    private String title;          // "Первый шаг"

    @Column(nullable = false)
    private String description;    // "Сделайте первую запись"

    @Column(nullable = false)
    private Integer points;        // очки за ачивку

    @Column(nullable = false)
    private String triggerType;    // тип условия

    private String triggerValue;   // значение условия

    private String iconUrl;        // ссылка на иконку: "/icons/achievements/first_recording.svg"

    @Builder.Default
    private Boolean active = true; // можно отключить без удаления

    private Instant createdAt;
}
