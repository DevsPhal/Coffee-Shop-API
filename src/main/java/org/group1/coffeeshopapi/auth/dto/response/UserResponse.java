package org.group1.coffeeshopapi.auth.dto.response;

import lombok.Builder;
import lombok.Data;
import org.group1.coffeeshopapi.common.enums.Role;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private Role role;
}
