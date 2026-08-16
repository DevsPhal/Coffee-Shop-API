package org.group1.coffeeshopapi.user.service;


import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.responses.PaginatedResponse;
import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser();

    UserResponse getUserById(UUID uId);

    PaginatedResponse<UserResponse> getAllUsers(Pageable pageable);

    void delete(UUID uid);

    UserResponse updateRole(UUID uid, Role role);
}
