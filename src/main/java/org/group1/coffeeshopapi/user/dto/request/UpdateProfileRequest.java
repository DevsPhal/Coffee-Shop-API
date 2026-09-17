package org.group1.coffeeshopapi.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;
import org.group1.coffeeshopapi.common.enums.Gender;
import org.springframework.scheduling.annotation.Schedules;

// Self-service profile edit — email and password change through their own dedicated flows
// (verification/OTP), so they're deliberately not here.
public record UpdateProfileRequest(
        @NotBlank(message = "Full name is required")
        @Schema(example = "Customer")
        String fullName,

        @Pattern(regexp = ValidationPatterns.CAMBODIA_PHONE_REGEX, message = ValidationPatterns.CAMBODIA_PHONE_MESSAGE)
        @Schema(example = "072 345 5674")
        String phoneNumber,

        Gender gender
) {
}
