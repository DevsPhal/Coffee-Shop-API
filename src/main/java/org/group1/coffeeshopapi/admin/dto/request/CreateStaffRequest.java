package org.group1.coffeeshopapi.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;
import org.group1.coffeeshopapi.common.enums.Gender;

public record CreateStaffRequest(
        @NotBlank(message = "Full name is required")
        @Schema(example = "Sophal Nem")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Schema(example = "user@gmail.com")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = ValidationPatterns.STRONG_PASSWORD_REGEX, message = ValidationPatterns.STRONG_PASSWORD_MESSAGE)
        @Schema(example = "Qwert!12@")
        String password,

        @Pattern(regexp = ValidationPatterns.CAMBODIA_PHONE_REGEX, message = ValidationPatterns.CAMBODIA_PHONE_MESSAGE)
        @Schema(example = "072 345 5674")
        String phoneNumber,

        Gender gender
) {
}
