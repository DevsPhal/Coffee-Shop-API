package org.group1.coffeeshopapi.admin.service;


import org.group1.coffeeshopapi.auth.dto.response.UserResponse;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.responses.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser();

    UserResponse getUserById(UUID uId);

    PageResponse<UserResponse> getAllUsers(Pageable pageable);

    void delete(UUID uid);

    UserResponse updateRole(UUID uid, Role role);
}