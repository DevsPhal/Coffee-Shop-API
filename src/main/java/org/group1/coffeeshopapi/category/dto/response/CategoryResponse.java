package org.group1.coffeeshopapi.category.dto.response;

import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.Status;

import java.time.LocalDateTime;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        Status status,
        UUID createdBy,
        String createdByName,
        Role createdByRole,
        UUID updatedBy,
        String updatedByName,
        Role updatedByRole,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}