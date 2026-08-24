package org.group1.coffeeshopapi.admin.service;

import org.group1.coffeeshopapi.admin.dto.request.CreateStaffRequest;
import org.group1.coffeeshopapi.admin.dto.request.UpdateStaffRequest;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Generic CRUD for staff accounts (ADMIN, BARISTA), scoped by role so a caller managing one
 * role can never read/modify/delete an account of a different role through the same endpoint.
 */
public interface StaffService {
    UserResponse create(CreateStaffRequest request, Role role, UUID createdBy);
    UserResponse getById(UUID id, Role role);
    Page<UserResponse> list(Role role, Pageable pageable);
    UserResponse update(UUID id, UpdateStaffRequest request, Role role);
    void delete(UUID id, Role role);
}