package kz.noisemap.userservice.service;

import kz.noisemap.common.exception.UnauthorizedException;
import kz.noisemap.userservice.dto.AuthDto;
import kz.noisemap.userservice.model.Role;
import kz.noisemap.userservice.model.User;
import kz.noisemap.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — unit tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock EmailService emailService;

    @InjectMocks AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("$2a$10$hashed")
                .displayName("Test User")
                .role(Role.USER)
                .language("ru")
                .active(true)
                .build();
    }

    // === Email нормализация ===

    @Test
    @DisplayName("Регистрация: email нормализуется в lowercase")
    void register_normalizesEmailToLowercase() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtService.getAccessExpirationMs()).thenReturn(3600000L);

        AuthDto.RegisterRequest request = AuthDto.RegisterRequest.builder()
                .email("USER@EXAMPLE.COM")
                .password("Pass123!")
                .displayName("Test User")
                .language("ru")
                .build();

        authService.register(request);

        // Проверяем что в БД сохранился lowercase email
        verify(userRepository).save(argThat(u -> "user@example.com".equals(u.getEmail())));
    }

    @Test
    @DisplayName("Регистрация: email trimmed (пробелы убраны)")
    void register_trimsEmailWhitespace() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtService.getAccessExpirationMs()).thenReturn(3600000L);

        AuthDto.RegisterRequest request = AuthDto.RegisterRequest.builder()
                .email("  user@example.com  ")
                .password("Pass123!")
                .displayName("Test User")
                .language("ru")
                .build();

        authService.register(request);

        verify(userRepository).save(argThat(u -> "user@example.com".equals(u.getEmail())));
    }

    @Test
    @DisplayName("Регистрация: дублирующий email бросает исключение")
    void register_throwsOnDuplicateEmail() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                AuthDto.RegisterRequest.builder()
                        .email("TEST@EXAMPLE.COM") // даже в другом регистре
                        .password("Pass123!")
                        .displayName("Test")
                        .language("ru")
                        .build()
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("already registered");
    }

    // === Логин ===

    @Test
    @DisplayName("Логин: успешная аутентификация")
    void login_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "$2a$10$hashed")).thenReturn(true);
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtService.getAccessExpirationMs()).thenReturn(3600000L);

        AuthDto.TokenResponse response = authService.login(
                AuthDto.LoginRequest.builder()
                        .email("test@example.com")
                        .password("password")
                        .build()
        );

        assertThat(response.getAccessToken()).isEqualTo("access");
    }

    @Test
    @DisplayName("Логин: неверный пароль → 401 UnauthorizedException")
    void login_wrongPassword_throws401() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                AuthDto.LoginRequest.builder()
                        .email("test@example.com")
                        .password("wrong")
                        .build()
        )).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Логин: несуществующий email → 401 (не 404, защита от enumeration)")
    void login_nonExistentEmail_throws401NotNotFoundException() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                AuthDto.LoginRequest.builder()
                        .email("nobody@example.com")
                        .password("any")
                        .build()
        )).isInstanceOf(UnauthorizedException.class);
    }

    // === Email enumeration protection ===

    @Test
    @DisplayName("forgot-password: несуществующий email → одинаковый ответ (не бросает)")
    void forgotPassword_nonExistentEmail_returnsGenericMessage() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        // НЕ должен бросать исключение
        AuthDto.ResetPasswordResponse response =
                authService.requestPasswordReset("nobody@example.com");

        assertThat(response.getMessage()).contains("If the email exists");
        // email-сервис НЕ должен вызываться
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("forgot-password: существующий email → отправляет письмо")
    void forgotPassword_existingEmail_sendsEmail() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generatePasswordResetToken(any())).thenReturn("reset-token-123");

        authService.requestPasswordReset("test@example.com");

        verify(emailService).sendPasswordResetEmail(
                eq("test@example.com"),
                eq("Test User"),
                eq("reset-token-123")
        );
    }

    @Test
    @DisplayName("forgot-password: ответ одинаковый для existing и non-existing email")
    void forgotPassword_sameResponseRegardlessOfEmailExistence() {
        // Существующий email
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generatePasswordResetToken(any())).thenReturn("token");

        AuthDto.ResetPasswordResponse responseForExisting =
                authService.requestPasswordReset("test@example.com");

        // Несуществующий email
        when(userRepository.findByEmail("nobody@nowhere.com")).thenReturn(Optional.empty());

        AuthDto.ResetPasswordResponse responseForNonExistent =
                authService.requestPasswordReset("nobody@nowhere.com");

        // Оба ответа одинаковые
        assertThat(responseForExisting.getMessage())
                .isEqualTo(responseForNonExistent.getMessage());
    }

    // === Reset password ===

    @Test
    @DisplayName("resetPassword: новый пароль не должен совпадать со старым")
    void resetPassword_sameAsOld_throwsException() {
        UUID userId = testUser.getId();
        when(jwtService.parsePasswordResetToken("reset-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("SamePass123!", "$2a$10$hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.resetPassword(
                AuthDto.SetNewPasswordRequest.builder()
                        .resetToken("reset-token")
                        .newPassword("SamePass123!")
                        .build()
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("different");
    }

    @Test
    @DisplayName("resetPassword: успешный сброс")
    void resetPassword_success() {
        UUID userId = testUser.getId();
        when(jwtService.parsePasswordResetToken("reset-token")).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("NewPass123!", "$2a$10$hashed")).thenReturn(false);
        when(passwordEncoder.encode("NewPass123!")).thenReturn("$2a$10$newHash");

        assertThatCode(() -> authService.resetPassword(
                AuthDto.SetNewPasswordRequest.builder()
                        .resetToken("reset-token")
                        .newPassword("NewPass123!")
                        .build()
        )).doesNotThrowAnyException();

        verify(userRepository).save(argThat(u -> "$2a$10$newHash".equals(u.getPasswordHash())));
    }
}
