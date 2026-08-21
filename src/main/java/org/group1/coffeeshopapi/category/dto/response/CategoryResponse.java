package org.group1.coffeeshopapi.category.dto.response;

import org.group1.coffeeshopapi.common.enums.Status;

import java.time.LocalDateTime;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        Status status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}