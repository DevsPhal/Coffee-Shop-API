package org.group1.coffeeshopapi.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyRegistrationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Schema(example = "user@gmail.com")
        String email,

        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "\\d{6}", message = "Verification code must be 6 digits")
        @Schema(example = "123456")
        String otp
) {
}