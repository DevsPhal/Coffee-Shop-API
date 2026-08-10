package org.group1.coffeeshopapi.admin.service;


import org.group1.coffeeshopapi.auth.dto.response.UserResponse;
import org.group1.coffeeshopapi.common.enums.Role;

import java.util.List;

public interface UserService {

    UserResponse getCurrentUser();

    UserResponse getUserById(Long uId);

    List<UserResponse> getAllUsers();

    void delete(Long uid);

    UserResponse updateRole(Long uid, Role role);
}