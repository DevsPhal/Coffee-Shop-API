package org.group1.coffeeshopapi.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;
import org.group1.coffeeshopapi.common.enums.Gender;
import org.group1.coffeeshopapi.common.enums.UserStatus;

/**
 * Partial update — any field left {@code null} is left unchanged. Email and password are
 * deliberately not editable here; those go through the dedicated auth flows.
 */
public record UpdateStaffRequest(
        String fullName,

        @Pattern(regexp = ValidationPatterns.CAMBODIA_PHONE_REGEX, message = ValidationPatterns.CAMBODIA_PHONE_MESSAGE)
        @Schema(example = "072 345 5674")
        String phoneNumber,

        Gender gender,
        UserStatus status
) {
}