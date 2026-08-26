package org.group1.coffeeshopapi.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;

public record ResetPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Schema(example = "user@gmail.com")
        String email,

        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "\\d{6}", message = "Verification code must be 6 digits")
        @Schema(example = "123456")
        String otp,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = ValidationPatterns.STRONG_PASSWORD_REGEX, message = ValidationPatterns.STRONG_PASSWORD_MESSAGE)
        @Schema(example = "Qwert!12@")
        String newPassword
) {
}