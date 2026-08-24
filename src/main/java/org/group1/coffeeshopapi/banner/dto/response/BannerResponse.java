package org.group1.coffeeshopapi.banner.dto.response;

import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.Status;

import java.time.LocalDateTime;
import java.util.UUID;

public record BannerResponse(
        UUID id,
        String title,
        String imageUrl,
        String linkUrl,
        Integer sortOrder,
        Status status,
        UUID adminId,
        String adminName,
        Role adminRole,
        UUID updatedByAdminId,
        String updatedByAdminName,
        Role updatedByAdminRole,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
