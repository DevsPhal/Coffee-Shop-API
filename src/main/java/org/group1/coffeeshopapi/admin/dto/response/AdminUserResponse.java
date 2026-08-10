package org.group1.coffeeshopapi.admin.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminUserResponse(
        String id,
        String username,
        String email,
        String givenName,
        String familyName,
        String phoneNumber,
        boolean enabled,
        boolean accountNonLocked,
        List<String> roles,
        LocalDateTime createdAt) {
}
