package kz.noisemap.userservice.service;

import kz.noisemap.userservice.dto.AuthDto;
import kz.noisemap.userservice.model.Role;
import kz.noisemap.userservice.model.User;
import kz.noisemap.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return generateTokens(user);
    }

    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!user.getActive()) {
            throw new IllegalStateException("Account is deactivated");
        }

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
