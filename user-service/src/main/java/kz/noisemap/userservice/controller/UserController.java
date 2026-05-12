package kz.noisemap.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.noisemap.userservice.dto.PublicUserDto;
import kz.noisemap.userservice.dto.UserDto;
import kz.noisemap.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Пользователи", description = "Профиль и настройки")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Получить свой профиль")
    public ResponseEntity<UserDto.Response> getProfile(
            @Parameter(description = "UUID пользователя") @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "Обновить профиль")
    public ResponseEntity<UserDto.Response> updateProfile(
            @Parameter(description = "UUID пользователя") @RequestHeader("X-User-Id") UUID userId,
            @RequestBody UserDto.UpdateRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    /**
     * Публичный endpoint — только displayName.
     * Используется gamification-service для лидерборда.
     */
    @GetMapping("/{userId}/public")
    @Operation(summary = "Публичные данные пользователя",
            description = "Возвращает только публичную информацию: id и displayName. "
                    + "Используется для лидерборда.")
    public ResponseEntity<PublicUserDto> getPublicInfo(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getPublicInfo(userId));
    }
}