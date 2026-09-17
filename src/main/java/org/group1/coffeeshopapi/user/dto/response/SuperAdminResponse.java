package org.group1.coffeeshopapi.user.dto.response;

import lombok.Builder;
import org.group1.coffeeshopapi.common.enums.Gender;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;

import java.util.UUID;

// Profile shape for the config-driven super admin. phoneNumber/avatarUrl/gender always come back
// null — there's no real account row to hold them.
@Builder
public record SuperAdminResponse(
        UUID id,
        String fullName,
        String email,
        String phoneNumber,
        String avatarUrl,
        Gender gender,
        Role role,
        UserStatus status
) {
}
