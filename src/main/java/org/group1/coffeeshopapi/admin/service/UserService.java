package org.group1.coffeeshopapi.admin.service;


import org.group1.coffeeshopapi.auth.dto.response.UserResponse;
import org.group1.coffeeshopapi.common.enums.Role;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser();

    UserResponse getUserById(UUID uId);

    List<UserResponse> getAllUsers();

    void delete(UUID uid);

    UserResponse updateRole(UUID uid, Role role);
}