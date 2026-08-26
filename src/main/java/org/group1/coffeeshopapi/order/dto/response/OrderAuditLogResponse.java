package org.group1.coffeeshopapi.order.dto.response;

import org.group1.coffeeshopapi.common.enums.OrderAuditAction;
import org.group1.coffeeshopapi.common.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderAuditLogResponse(
        UUID id,
        OrderAuditAction action,
        UUID actorId,
        String actorName,
        Role actorRole,
        LocalDateTime createdAt
) {
}
