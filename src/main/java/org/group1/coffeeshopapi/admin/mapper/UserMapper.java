package org.group1.coffeeshopapi.admin.mapper;

import org.group1.coffeeshopapi.admin.entity.User;
import org.group1.coffeeshopapi.auth.dto.response.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}