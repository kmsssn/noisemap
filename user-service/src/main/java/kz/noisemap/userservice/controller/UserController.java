package kz.noisemap.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.noisemap.userservice.dto.UserDto;
import kz.noisemap.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Пользователи", description = "Профиль, настройки, калибровка устройства")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Получить свой профиль",
               description = "Возвращает данные текущего пользователя. X-User-Id прокидывается API Gateway.")
    public ResponseEntity<UserDto.Response> getProfile(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "Обновить профиль", description = "Изменить отображаемое имя или язык интерфейса")
    public ResponseEntity<UserDto.Response> updateProfile(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody UserDto.UpdateRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PutMapping("/me/device")
    @Operation(summary = "Обновить данные устройства",
               description = "Установить модель устройства и калибровочный offset в дБ. "
                       + "Offset применяется к измерениям для компенсации погрешности микрофона.")
    public ResponseEntity<UserDto.Response> updateDevice(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody UserDto.DeviceUpdateRequest request) {
        return ResponseEntity.ok(userService.updateDevice(userId, request));
    }

    @GetMapping("/{userId}/calibration")
    @Operation(summary = "Получить калибровку устройства (internal)",
               description = "Внутренний endpoint для других сервисов. Возвращает калибровочный offset.")
    public ResponseEntity<Double> getCalibration(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getCalibrationOffset(userId));
    }
}
