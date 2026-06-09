package kz.noisemap.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.noisemap.userservice.dto.AuthDto;
import kz.noisemap.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Регистрация, логин, сброс пароля, смена email")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Регистрация",
            description = "Создаёт аккаунт и возвращает JWT токены. " +
                    "Пароль: минимум 8 символов, 1 заглавная буква, 1 цифра, 1 спецсимвол. " +
                    "Email нормализуется в lowercase.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь создан"),
            @ApiResponse(responseCode = "400", description = "Email уже занят или невалидные данные")
    })
    public ResponseEntity<AuthDto.TokenResponse> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Вход", description = "Возвращает JWT токены")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный вход"),
            @ApiResponse(responseCode = "401", description = "Неверный email или пароль"),
            @ApiResponse(responseCode = "409", description = "Аккаунт деактивирован")
    })
    public ResponseEntity<AuthDto.TokenResponse> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Забыл пароль",
            description = "Генерирует токен для сброса пароля. " +
                    "Токен НЕ возвращается в ответе (security) — в продакшене " +
                    "отправляется по email, в MVP логируется на сервере.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Если email существует — токен сгенерирован и отправлен")
    })
    public ResponseEntity<AuthDto.ResetPasswordResponse> forgotPassword(
            @Valid @RequestBody AuthDto.ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.requestPasswordReset(request.getEmail()));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Сбросить пароль",
            description = "Устанавливает новый пароль по токену из forgot-password. " +
                    "Новый пароль не должен совпадать с текущим.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пароль изменён"),
            @ApiResponse(responseCode = "400", description = "Новый пароль совпадает со старым"),
            @ApiResponse(responseCode = "401", description = "Невалидный или истёкший токен")
    })
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody AuthDto.SetNewPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-password")
    @Operation(summary = "Сменить пароль (залогиненный)",
            description = "Смена пароля для залогиненного юзера. " +
                    "Требует текущий пароль для подтверждения.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пароль изменён"),
            @ApiResponse(responseCode = "400", description = "Новый пароль совпадает со старым"),
            @ApiResponse(responseCode = "401", description = "Неверный текущий пароль")
    })
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "UUID пользователя") @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody AuthDto.ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-email")
    @Operation(summary = "Сменить email",
            description = "Смена email пользователя. Требует подтверждения текущим паролем. " +
                    "Возвращает новые токены, т.к. email включён в JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email изменён, возвращены новые токены"),
            @ApiResponse(responseCode = "400", description = "Email уже занят или совпадает с текущим"),
            @ApiResponse(responseCode = "401", description = "Неверный пароль")
    })
    public ResponseEntity<AuthDto.TokenResponse> changeEmail(
            @Parameter(description = "UUID пользователя") @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody AuthDto.ChangeEmailRequest request) {
        return ResponseEntity.ok(authService.changeEmail(userId, request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновить токены",
            description = "Принимает refresh token, возвращает новую пару access + refresh токенов.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токены обновлены"),
            @ApiResponse(responseCode = "401", description = "Невалидный или истёкший refresh token")
    })
    public ResponseEntity<AuthDto.TokenResponse> refresh(
            @Valid @RequestBody AuthDto.RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }
}