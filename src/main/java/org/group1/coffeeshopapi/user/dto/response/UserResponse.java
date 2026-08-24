package org.group1.coffeeshopapi.user.dto.response;

import lombok.Builder;
import org.group1.coffeeshopapi.common.enums.Gender;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.Status;

import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String phoneNumber,
        String avatarUrl,
        Gender gender,
        Role role,
        Status status,
        boolean telegramLinked,
        // Which admin/super admin created this account. Only ever set for ADMIN/BARISTA rows —
        // customers self-register, so this stays null for them.
        UUID createdBy,
        String createdByName,
        Role createdByRole
) {
}