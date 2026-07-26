package org.group1.coffeeshopapi.service;


import org.group1.coffeeshopapi.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse getCurrentUser();

    UserResponse getUserById(UUID uId);

    List<UserResponse> getAllUsers();

    void deleteUser(UUID uid);
}