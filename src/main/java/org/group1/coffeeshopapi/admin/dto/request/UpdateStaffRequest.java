package org.group1.coffeeshopapi.admin.dto.request;

import jakarta.validation.constraints.Pattern;
import org.group1.coffeeshopapi.common.enums.Gender;
import org.group1.coffeeshopapi.common.enums.UserStatus;

/**
 * Partial update — any field left {@code null} is left unchanged. Email and password are
 * deliberately not editable here; those go through the dedicated auth flows.
 */
public record UpdateStaffRequest(
        String fullName,

        @Pattern(regexp = "\\d{9,10}", message = "Phone number must be 9 or 10 digits")
        String phoneNumber,

        Gender gender,
        UserStatus status
) {
}