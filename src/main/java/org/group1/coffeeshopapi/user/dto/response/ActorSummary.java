package org.group1.coffeeshopapi.user.dto.response;

import org.group1.coffeeshopapi.common.enums.Role;

import java.util.UUID;

public record ActorSummary(
        UUID id,
        String name,
        Role role
) {
}
