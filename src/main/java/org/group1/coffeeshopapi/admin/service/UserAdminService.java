package org.group1.coffeeshopapi.admin.service;

import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Super-admin-only view across every account regardless of role (ADMIN, BARISTA, CUSTOMER).
 * Unlike {@link StaffService}, which is scoped to a single role at a time, this reads across
 * all three tables at once.
 */
public interface UserAdminService {
    Page<UserResponse> list(Role roleFilter, Pageable pageable);
    UserResponse getById(UUID id);
}