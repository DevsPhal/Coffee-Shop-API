package org.group1.coffeeshopapi.admin.dto.request;

import org.group1.coffeeshopapi.common.enums.Gender;
import org.group1.coffeeshopapi.common.enums.Status;

/**
 * Partial update — any field left {@code null} is left unchanged. Email and password are
 * deliberately not editable here; those go through the dedicated auth flows.
 */
public record UpdateStaffRequest(
        String fullName,
        String phoneNumber,
        Gender gender,
        Status status
) {
}