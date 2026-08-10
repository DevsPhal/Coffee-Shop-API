package org.group1.coffeeshopapi.auth.dto.response;

import lombok.Builder;
import lombok.Data;
import org.group1.coffeeshopapi.common.enums.Role;

import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private Role role;
}
