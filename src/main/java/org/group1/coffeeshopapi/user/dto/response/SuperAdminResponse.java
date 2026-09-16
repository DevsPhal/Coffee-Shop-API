package org.group1.coffeeshopapi.user.dto.response;

import lombok.Builder;
import org.group1.coffeeshopapi.common.enums.Gender;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;

import java.util.UUID;

/**
 * Profile shape for the config-driven super admin. Leaner than {@link UserResponse} — there is no
 * {@code User} row behind it, so it has no Telegram link and no creator. Every field but
 * {@code id}/{@code email}/{@code role}/{@code status} is fixed rather than editable: the email
 * comes from configuration, the rest ({@code phoneNumber}, {@code avatarUrl}, {@code gender})
 * simply have nothing to hold and always come back null — see
 * {@link org.group1.coffeeshopapi.common.security.SuperAdminUserDetails#toResponse()}.
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
