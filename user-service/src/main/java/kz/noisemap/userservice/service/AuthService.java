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
    private final EmailService emailService;

    private String normalizeEmail(String email) {
        if (email == null) return null;
        return email.trim().toLowerCase();
    }

    @Transactional
    public AuthDto.TokenResponse register(AuthDto.RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName().trim())
                .role(Role.USER)
                .language(request.getLanguage() != null ? request.getLanguage() : "ru")
                .deviceModel(request.getDeviceModel())
                .build();

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());
        return generateTokens(user);
    }

    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(email)
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


    @Transactional
    public AuthDto.ResetPasswordResponse requestPasswordReset(String email) {
        String normalizedEmail = normalizeEmail(email);

        userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
            String resetToken = jwtService.generatePasswordResetToken(user);

            // Асинхронная отправка письма — не блокирует HTTP запрос
            emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    user.getDisplayName(),
                    resetToken
            );

            log.info("Password reset requested for: {}", normalizedEmail);
        });


        return AuthDto.ResetPasswordResponse.builder()
                .message("If the email exists, a password reset link has been sent")
                .build();
    }


    @Transactional
    public void resetPassword(AuthDto.SetNewPasswordRequest request) {
        UUID userId = jwtService.parsePasswordResetToken(request.getResetToken());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password reset for user: {}", user.getEmail());
    }


    @Transactional
    public void changePassword(UUID userId, AuthDto.ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }


    @Transactional
    public AuthDto.TokenResponse changeEmail(UUID userId, AuthDto.ChangeEmailRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }

        String newEmail = normalizeEmail(request.getNewEmail());

        if (newEmail.equals(user.getEmail())) {
            throw new IllegalArgumentException("New email must be different from the current");
        }

        if (userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email already in use");
        }

        user.setEmail(newEmail);
        userRepository.save(user);
        log.info("Email changed for user: {} -> {}", userId, newEmail);

        return generateTokens(user);
    }

    private AuthDto.TokenResponse generateTokens(User user) {
        return AuthDto.TokenResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .expiresIn(jwtService.getAccessExpirationMs() / 1000)
                .build();
    }
}