package kz.noisemap.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.noisemap.userservice.dto.DeviceCalibrationDto;
import kz.noisemap.userservice.service.DeviceCalibrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "Справочник устройств",
        description = "Калибровочные коэффициенты для разных моделей телефонов")
public class DeviceCalibrationController {

    private final DeviceCalibrationService service;

    /**
     * Internal endpoint для recording-service.
     * Возвращает калибровку для устройства; если устройство неизвестно — создаёт запись.
     */
    @GetMapping("/calibration")
    @Operation(summary = "Калибровка устройства (internal)",
            description = "Возвращает offset для модели устройства. " +
                    "Если устройство неизвестно — автоматически добавляется в справочник с offset=0.0 " +
                    "и помечается как unverified. Используется recording-service при загрузке записей.")
    public ResponseEntity<DeviceCalibrationDto.CalibrationResult> getCalibration(
            @RequestParam("model") String model) {
        return ResponseEntity.ok(service.getCalibrationOrCreate(model));
    }

    @GetMapping
    @Operation(summary = "Список устройств с пагинацией")
    public ResponseEntity<Page<DeviceCalibrationDto.Response>> list(
            @RequestParam(required = false) String manufacturer,
            @PageableDefault(size = 20) Pageable pageable) {
        if (manufacturer != null && !manufacturer.isBlank()) {
            return ResponseEntity.ok(service.getByManufacturer(manufacturer, pageable));
        }
        return ResponseEntity.ok(service.getAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Детали калибровки устройства")
    public ResponseEntity<DeviceCalibrationDto.Response> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @Operation(summary = "Добавить устройство в справочник",
            description = "Только для админов. Создаёт новую запись с подтверждённой калибровкой.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Устройство добавлено"),
            @ApiResponse(responseCode = "400", description = "Модель уже существует")
    })
    public ResponseEntity<DeviceCalibrationDto.Response> create(
            @Valid @RequestBody DeviceCalibrationDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить калибровку устройства",
            description = "Только для админов. Используется для проставления правильного offset " +
                    "у автоматически созданных записей.")
    public ResponseEntity<DeviceCalibrationDto.Response> update(
            @PathVariable UUID id,
            @Valid @RequestBody DeviceCalibrationDto.UpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить устройство из справочника")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}