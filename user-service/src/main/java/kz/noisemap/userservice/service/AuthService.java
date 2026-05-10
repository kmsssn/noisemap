package kz.noisemap.userservice.service;

import kz.noisemap.common.exception.UnauthorizedException;
import kz.noisemap.userservice.dto.AuthDto;
import kz.noisemap.userservice.model.Role;
import kz.noisemap.userservice.model.User;
import kz.noisemap.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthDto.TokenResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .role(Role.USER)
                .language(request.getLanguage() != null ? request.getLanguage() : "ru")
                .deviceModel(request.getDeviceModel())
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());
        return generateTokens(user);
    }

    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // Неверный пароль → 401, не 500
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (!user.getActive()) {
            throw new SecurityException("Account is deactivated");
        }

        return generateTokens(user);
    }

    /**
     * Сброс пароля — генерирует временный токен.
     * В реальной системе нужно отправлять email.
     * Для MVP возвращает токен напрямую.
     */
    @Transactional
    public AuthDto.ResetPasswordResponse requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));

        // Генерируем одноразовый токен для сброса пароля (живёт 1 час)
        String resetToken = jwtService.generatePasswordResetToken(user);

        log.info("Password reset requested for: {}", email);

        // TODO: отправить email с токеном
        // emailService.sendPasswordReset(email, resetToken);

        return AuthDto.ResetPasswordResponse.builder()
                .message("Password reset token generated. Check your email.")
                .resetToken(resetToken) // в продакшене убрать из ответа, только через email
                .build();
    }

    /**
     * Установить новый пароль по токену сброса.
     */
    @Transactional
    public void resetPassword(AuthDto.SetNewPasswordRequest request) {
        UUID userId = jwtService.parsePasswordResetToken(request.getResetToken());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password reset for user: {}", user.getEmail());
    }

    private AuthDto.TokenResponse generateTokens(User user) {
        return AuthDto.TokenResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .build();
    }
}
