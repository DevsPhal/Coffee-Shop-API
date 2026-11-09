package org.group1.coffeeshopapi.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;
import org.group1.coffeeshopapi.common.enums.Gender;
import jakarta.validation.constraints.Size;

public record InviteStaffRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        @Schema(example = "Chanden Sok")
        String fullName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = ValidationPatterns.CAMBODIA_PHONE_REGEX, message = ValidationPatterns.CAMBODIA_PHONE_MESSAGE)
        @Schema(example = "072 345 5674")
        String phoneNumber,

        Gender gender
) {
}
