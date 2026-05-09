package kz.noisemap.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Аутентификация", description = "Регистрация, логин, сброс пароля")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Регистрация", description = "Создаёт аккаунт и возвращает JWT токены")
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
               description = "Генерирует токен для сброса пароля. "
                       + "В продакшене отправляет email, для MVP возвращает токен напрямую.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токен сгенерирован"),
            @ApiResponse(responseCode = "400", description = "Email не найден")
    })
    public ResponseEntity<AuthDto.ResetPasswordResponse> forgotPassword(
            @Valid @RequestBody AuthDto.ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.requestPasswordReset(request.getEmail()));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Сбросить пароль",
               description = "Устанавливает новый пароль по токену из forgot-password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Пароль изменён"),
            @ApiResponse(responseCode = "401", description = "Невалидный или истёкший токен")
    })
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody AuthDto.SetNewPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
