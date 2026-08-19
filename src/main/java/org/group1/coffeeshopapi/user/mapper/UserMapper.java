package org.group1.coffeeshopapi.user.mapper;

import org.group1.coffeeshopapi.user.dto.response.UserResponse;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        // Telegram linking is a customer-only concern — Admin/Barista never have a chat linked.
        boolean telegramLinked = user instanceof Customer customer && customer.getTelegramChatId() != null;

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .gender(user.getGender())
                .role(user.getRole())
                .status(user.getStatus())
                .telegramLinked(telegramLinked)
                .build();
    }
}