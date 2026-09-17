package org.group1.coffeeshopapi.admin.service;

import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.user.dto.request.UpdateProfileRequest;
import org.group1.coffeeshopapi.user.dto.request.UpdateUserStatusRequest;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

// Super-admin-only view across every account regardless of role (admin, barista, customer).
public interface UserAdminService {
    Page<UserResponse> list(Role roleFilter, Pageable pageable);
    UserResponse getById(UUID id);
    UserResponse update(UUID id, UpdateProfileRequest request);
    void delete(UUID id);

    // Suspend/ban/soft-delete any account, including customers.
    UserResponse updateStatus(UUID id, UpdateUserStatusRequest request);
}
