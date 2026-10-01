package org.group1.coffeeshopapi.admin.service;

import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.user.dto.request.UpdateProfileRequest;
import org.group1.coffeeshopapi.user.dto.request.UpdateUserStatusRequest;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserAdminService {
    Page<UserResponse> list(Role roleFilter, Pageable pageable);
    UserResponse getById(UUID id);
    UserResponse update(UUID id, UpdateProfileRequest request);
    void delete(UUID id);

    UserResponse updateStatus(UUID id, UpdateUserStatusRequest request);
}
