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
@Tag(name = "Аутентификация", description = "Регистрация, логин, получение JWT токенов")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Регистрация нового пользователя",
               description = "Создаёт аккаунт и возвращает пару JWT токенов (access + refresh)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь создан, токены выданы"),
            @ApiResponse(responseCode = "400", description = "Невалидные данные или email уже занят")
    })
    public ResponseEntity<AuthDto.TokenResponse> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Вход в систему",
               description = "Аутентификация по email и паролю. Возвращает JWT токены.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный вход"),
            @ApiResponse(responseCode = "400", description = "Неверные учётные данные"),
            @ApiResponse(responseCode = "409", description = "Аккаунт деактивирован")
    })
    public ResponseEntity<AuthDto.TokenResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
