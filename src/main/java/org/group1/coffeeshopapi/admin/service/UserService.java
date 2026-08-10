package org.group1.coffeeshopapi.admin.service;

import java.util.List;

import org.group1.coffeeshopapi.auth.dto.response.UserResponse;

public interface UserService {

    UserResponse getCurrentUser();

    UserResponse getUserById(Long uId);

    List<UserResponse> getAllUsers();

    void delete(Long uid);
}