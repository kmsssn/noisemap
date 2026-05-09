package kz.noisemap.common.exception;

/**
 * Бросается при неверных учётных данных (неверный пароль, токен).
 * GlobalExceptionHandler возвращает HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
