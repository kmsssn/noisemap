package kz.noisemap.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDto {

    public static final String PASSWORD_PATTERN =
            "^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,100}$";
    public static final String PASSWORD_MESSAGE =
            "Пароль должен быть от 8 до 100 символов и содержать минимум: 1 заглавную букву, 1 цифру, 1 спецсимвол";

    public static final String DISPLAY_NAME_PATTERN = "^[\\p{L}\\p{N} ._-]+$";
    public static final String DISPLAY_NAME_MESSAGE =
            "Имя может содержать только буквы, цифры, пробелы, точки, дефисы и подчёркивания";

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Email обязателен")
        @Email(message = "Невалидный формат email")
        @Size(max = 255, message = "Email слишком длинный")
        private String email;

        @NotBlank(message = "Пароль обязателен")
        @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
        private String password;

        @NotBlank(message = "Имя обязательно")
        @Size(min = 2, max = 50, message = "Имя должно быть от 2 до 50 символов")
        @Pattern(regexp = DISPLAY_NAME_PATTERN, message = DISPLAY_NAME_MESSAGE)
        private String displayName;

        private String language;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private Long expiresIn;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ForgotPasswordRequest {
        @NotBlank @Email
        private String email;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ResetPasswordResponse {
        private String message;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SetNewPasswordRequest {
        @NotBlank
        private String resetToken;

        @NotBlank
        @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
        private String newPassword;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChangeEmailRequest {
        @NotBlank @Email
        @Size(max = 255)
        private String newEmail;

        @NotBlank(message = "Подтвердите паролем")
        private String currentPassword;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;

        @NotBlank
        @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
        private String newPassword;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RefreshTokenRequest {
        @NotBlank
        private String refreshToken;
    }
}