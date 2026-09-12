package org.group1.coffeeshopapi.user.dto.response;

import lombok.Builder;
import org.group1.coffeeshopapi.common.enums.Gender;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;

import java.util.UUID;

/**
 * Profile shape for the config-driven super admin. Leaner than {@link UserResponse} — there is no
 * {@code User} row behind it, so it has no Telegram link and no creator. The email is fixed by
 * configuration; the name, phone, gender and avatar are editable and kept in
 * {@code super_admin_profile}.
 */
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
