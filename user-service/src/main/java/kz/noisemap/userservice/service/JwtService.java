package kz.noisemap.userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kz.noisemap.common.exception.UnauthorizedException;
import kz.noisemap.userservice.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration-ms:3600000}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    private static final long RESET_EXPIRATION_MS = 3600000L; // 1 час

    public String generateAccessToken(User user) {
        return buildToken(user, accessExpirationMs, "access");
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpirationMs, "refresh");
    }

    public String generatePasswordResetToken(User user) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("type", "password_reset")
                .claim("email", user.getEmail())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + RESET_EXPIRATION_MS))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Парсит токен сброса пароля и возвращает userId.
     * Бросает UnauthorizedException если токен невалидный или не того типа.
     */
    public UUID parsePasswordResetToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!"password_reset".equals(claims.get("type"))) {
                throw new UnauthorizedException("Invalid reset token");
            }

            return UUID.fromString(claims.getSubject());
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid or expired reset token");
        }
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs;
    }

    private String buildToken(User user, long expirationMs, String type) {
        Date now = new Date();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("displayName", user.getDisplayName())
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
