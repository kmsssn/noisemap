package kz.noisemap.userservice.service;

import kz.noisemap.userservice.dto.DeviceCalibrationDto;
import kz.noisemap.userservice.model.DeviceCalibration;
import kz.noisemap.userservice.repository.DeviceCalibrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCalibrationService {

    private final DeviceCalibrationRepository repository;

    /**
     * Получить калибровку для устройства.
     *
     * Если устройство уже в справочнике → возвращаем его данные.
     * Если устройства нет → автоматически создаём запись с offset=0.0 и verified=false.
     *   Эта стратегия "авто-каталогизации" позволяет:
     *   - Сразу принимать запись пользователя (offset=0 → dBA без коррекции)
     *   - Накапливать список реально используемых устройств
     *   - Администратор позже может проставить правильный offset для популярных моделей
     */
    @Transactional
    public DeviceCalibrationDto.CalibrationResult getCalibrationOrCreate(String deviceModel) {
        if (deviceModel == null || deviceModel.isBlank()) {
            log.debug("Device model is blank — returning zero calibration");
            return DeviceCalibrationDto.CalibrationResult.builder()
                    .model("unknown")
                    .calibrationOffsetDb(0.0)
                    .verified(false)
                    .build();
        }

        String normalizedModel = deviceModel.trim();

        DeviceCalibration calibration = repository.findByModelIgnoreCase(normalizedModel)
                .orElseGet(() -> {
                    log.info("Auto-cataloguing new device model: {}", normalizedModel);
                    return repository.save(DeviceCalibration.builder()
                            .manufacturer(guessManufacturer(normalizedModel))
                            .model(normalizedModel)
                            .calibrationOffsetDb(0.0)
                            .source("auto")
                            .verified(false)
                            .sampleCount(0)
                            .build());
                });

        // Увеличиваем счётчик использований (помогает приоритезировать калибровку)
        repository.incrementSampleCount(calibration.getId());

        return DeviceCalibrationDto.CalibrationResult.builder()
                .model(calibration.getModel())
                .calibrationOffsetDb(calibration.getCalibrationOffsetDb())
                .verified(calibration.getVerified())
                .build();
    }

    public Page<DeviceCalibrationDto.Response> getAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    public Page<DeviceCalibrationDto.Response> getByManufacturer(String manufacturer, Pageable pageable) {
        return repository.findAllByManufacturerIgnoreCase(manufacturer, pageable).map(this::toResponse);
    }

    public DeviceCalibrationDto.Response getById(UUID id) {
        DeviceCalibration calibration = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device calibration not found: " + id));
        return toResponse(calibration);
    }

    @Transactional
    public DeviceCalibrationDto.Response create(DeviceCalibrationDto.CreateRequest request) {
        if (repository.existsByModelIgnoreCase(request.getModel())) {
            throw new IllegalArgumentException("Device model already exists: " + request.getModel());
        }

        DeviceCalibration calibration = DeviceCalibration.builder()
                .manufacturer(request.getManufacturer())
                .model(request.getModel())
                .calibrationOffsetDb(request.getCalibrationOffsetDb())
                .source(request.getSource() != null ? request.getSource() : "manual")
                .verified(request.getVerified() != null ? request.getVerified() : true)
                .sampleCount(0)
                .build();

        calibration = repository.save(calibration);
        log.info("Device calibration created: {} (offset={})",
                calibration.getModel(), calibration.getCalibrationOffsetDb());
        return toResponse(calibration);
    }

    @Transactional
    public DeviceCalibrationDto.Response update(UUID id, DeviceCalibrationDto.UpdateRequest request) {
        DeviceCalibration calibration = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Device calibration not found: " + id));

        if (request.getManufacturer() != null) {
            calibration.setManufacturer(request.getManufacturer());
        }
        if (request.getCalibrationOffsetDb() != null) {
            calibration.setCalibrationOffsetDb(request.getCalibrationOffsetDb());
        }
        if (request.getSource() != null) {
            calibration.setSource(request.getSource());
        }
        if (request.getVerified() != null) {
            calibration.setVerified(request.getVerified());
        }

        calibration = repository.save(calibration);
        log.info("Device calibration updated: {} (offset={}, verified={})",
                calibration.getModel(), calibration.getCalibrationOffsetDb(), calibration.getVerified());
        return toResponse(calibration);
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Device calibration not found: " + id);
        }
        repository.deleteById(id);
        log.info("Device calibration deleted: {}", id);
    }

    /**
     * Простая эвристика для определения производителя по модели.
     * Используется при авто-каталогизации.
     */
    private String guessManufacturer(String model) {
        String lower = model.toLowerCase();
        if (lower.contains("iphone") || lower.contains("ipad")) return "Apple";
        if (lower.contains("galaxy") || lower.contains("samsung")) return "Samsung";
        if (lower.contains("pixel")) return "Google";
        if (lower.contains("xiaomi") || lower.contains("redmi") || lower.contains("poco")) return "Xiaomi";
        if (lower.contains("huawei") || lower.contains("honor")) return "Huawei";
        if (lower.contains("oneplus")) return "OnePlus";
        if (lower.contains("realme")) return "Realme";
        if (lower.contains("oppo")) return "Oppo";
        if (lower.contains("vivo")) return "Vivo";
        return "Unknown";
    }

    private DeviceCalibrationDto.Response toResponse(DeviceCalibration calibration) {
        return DeviceCalibrationDto.Response.builder()
                .id(calibration.getId())
                .manufacturer(calibration.getManufacturer())
                .model(calibration.getModel())
                .calibrationOffsetDb(calibration.getCalibrationOffsetDb())
                .source(calibration.getSource())
                .verified(calibration.getVerified())
                .sampleCount(calibration.getSampleCount())
                .createdAt(calibration.getCreatedAt())
                .updatedAt(calibration.getUpdatedAt())
                .build();
    }
}