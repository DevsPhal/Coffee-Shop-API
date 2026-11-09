package org.group1.coffeeshopapi.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        @Schema(example = "Qwert!12@")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = ValidationPatterns.PASSWORD_MAX_LENGTH, message = "Password must be 8 to 72 characters")
        @Pattern(regexp = ValidationPatterns.STRONG_PASSWORD_REGEX, message = ValidationPatterns.STRONG_PASSWORD_MESSAGE)
        @Schema(example = "Asdfg!34#")
        String newPassword
) {
}
