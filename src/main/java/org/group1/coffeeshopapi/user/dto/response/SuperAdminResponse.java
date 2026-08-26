package org.group1.coffeeshopapi.user.dto.response;

import lombok.Builder;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.UserStatus;

import java.util.UUID;

/**
 * Profile shape for the config-driven super admin — deliberately leaner than {@link UserResponse}
 * since it has no gender, phone number, or Telegram link (it isn't backed by a {@code User} row).
 */
@Builder
public record SuperAdminResponse(UUID id, String fullName, String email, Role role, UserStatus status) {
}
