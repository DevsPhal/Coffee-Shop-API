package org.group1.coffeeshopapi.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;
import org.group1.coffeeshopapi.common.enums.Gender;

public record RegisterRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        @Schema(example = "Customer")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        @Schema(example = "user@gmail.com")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = ValidationPatterns.PASSWORD_MAX_LENGTH, message = "Password must be 8 to 72 characters")
        @Pattern(regexp = ValidationPatterns.STRONG_PASSWORD_REGEX, message = ValidationPatterns.STRONG_PASSWORD_MESSAGE)
        @Schema(example = "Qwert!12@")
        String password,

        @Pattern(regexp = ValidationPatterns.CAMBODIA_PHONE_REGEX, message = ValidationPatterns.CAMBODIA_PHONE_MESSAGE)
        @Schema(example = "072 345 5674")
        String phoneNumber,

        Gender gender
) {
}
