package org.group1.coffeeshopapi.admin.service;

import org.group1.coffeeshopapi.admin.dto.request.CreateStaffRequest;
import org.group1.coffeeshopapi.admin.dto.request.UpdateStaffRequest;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

/**
 * Generic CRUD for staff accounts (ADMIN, BARISTA), scoped by role so a caller managing one
 * role can never read/modify/delete an account of a different role through the same endpoint.
 */
public interface StaffService {
    UserResponse create(CreateStaffRequest request, Role role);
    UserResponse getById(UUID id, Role role);
    List<UserResponse> list(Role role);
    UserResponse update(UUID id, UpdateStaffRequest request, Role role);
    void delete(UUID id, Role role);
}