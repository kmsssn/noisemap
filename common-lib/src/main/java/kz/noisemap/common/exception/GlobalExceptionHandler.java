package kz.noisemap.common.exception;

import kz.noisemap.common.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, WebRequest req) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(IllegalStateException ex, WebRequest req) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    /**
     * SecurityException → 403 Forbidden (нет прав).
     * Для 401 (неверный пароль) используется UnauthorizedException.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(SecurityException ex, WebRequest req) {
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    /**
     * Неверные учётные данные → 401 Unauthorized.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex, WebRequest req) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest req) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request contains invalid fields")
                .path(req.getDescription(false))
                .timestamp(Instant.now())
                .validationErrors(errors)
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex, WebRequest req) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", req);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message, WebRequest req) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(req.getDescription(false))
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
