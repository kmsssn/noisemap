package kz.noisemap.userservice.repository;

import kz.noisemap.userservice.model.DeviceCalibration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceCalibrationRepository extends JpaRepository<DeviceCalibration, UUID> {

    Optional<DeviceCalibration> findByModelIgnoreCase(String model);

    boolean existsByModelIgnoreCase(String model);

    Page<DeviceCalibration> findAllByManufacturerIgnoreCase(String manufacturer, Pageable pageable);

    /**
     * Атомарно увеличиваем счётчик использований устройства.
     * Помогает приоритезировать какие устройства калибровать первыми.
     */
    @Modifying
    @Query("UPDATE DeviceCalibration d SET d.sampleCount = d.sampleCount + 1 WHERE d.id = :id")
    void incrementSampleCount(UUID id);
}