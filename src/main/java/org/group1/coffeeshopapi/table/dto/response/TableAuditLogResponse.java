package org.group1.coffeeshopapi.table.dto.response;

import org.group1.coffeeshopapi.common.enums.TableAuditAction;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;

import java.time.LocalDateTime;
import java.util.UUID;

// actor is null when the system made the change (a booking hold or an automatic no-show).
public record TableAuditLogResponse(
        UUID id,
        TableAuditAction action,
        TableStatus fromStatus,
        TableStatus toStatus,
        int guestCount,
        ActorSummary actor,
        boolean override,
        String reason,
        UUID orderId,
        UUID reservationId,
        LocalDateTime createdAt
) {
}
