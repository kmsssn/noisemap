package kz.noisemap.userservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**Справочник калибровок устройств.*/

@Entity
@Table(name = "device_calibrations", indexes = {
        @Index(name = "idx_device_model", columnList = "model", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCalibration {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 50)
    private String manufacturer;

    @Column(nullable = false, length = 100, unique = true)
    private String model;

    @Column(name = "calibration_offset_db", nullable = false)
    @Builder.Default
    private Double calibrationOffsetDb = 0.0;

    /**
     * Источник данных: "research", "manufacturer", "auto"
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String source = "auto";

    /**
     * Подтверждено ли значение администратором.
     * Записи с verified=false обычно созданы автоматически и требуют проверки.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean verified = false;

    /**
     * Помогает приоритезировать какие устройства калибровать первыми.
     */
    @Column(name = "sample_count", nullable = false)
    @Builder.Default
    private Integer sampleCount = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}