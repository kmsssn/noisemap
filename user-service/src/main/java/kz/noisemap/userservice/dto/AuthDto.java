package kz.noisemap.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank @Email
        private String email;
        @NotBlank @Size(min = 6, max = 100)
        private String password;
        @NotBlank @Size(min = 2, max = 50)
        private String displayName;
        private String language;
        private String deviceModel;
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
        private String resetToken; // в продакшене убрать, только для MVP
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SetNewPasswordRequest {
        @NotBlank
        private String resetToken;
        @NotBlank @Size(min = 6, max = 100)
        private String newPassword;
    }
}
