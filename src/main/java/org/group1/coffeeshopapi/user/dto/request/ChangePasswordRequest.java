package org.group1.coffeeshopapi.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;

// Password change for the signed-in account. Requires the current password so a hijacked
// session can't lock the real owner out.
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        @Schema(example = "Qwert!12@")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(regexp = ValidationPatterns.STRONG_PASSWORD_REGEX, message = ValidationPatterns.STRONG_PASSWORD_MESSAGE)
        @Schema(example = "Asdfg!34#")
        String newPassword
) {
}
