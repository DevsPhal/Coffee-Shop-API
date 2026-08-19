package org.group1.coffeeshopapi.user.dto.response;

import lombok.Builder;
import org.group1.coffeeshopapi.common.enums.Gender;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.Status;

import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String phoneNumber,
        Gender gender,
        Role role,
        Status status,
        boolean telegramLinked
) {
}