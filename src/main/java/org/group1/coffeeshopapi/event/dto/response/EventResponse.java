package org.group1.coffeeshopapi.event.dto.response;

import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.Status;

import java.time.LocalDateTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        String imageUrl,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Status status,
        UUID createdBy,
        String createdByName,
        Role createdByRole,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}