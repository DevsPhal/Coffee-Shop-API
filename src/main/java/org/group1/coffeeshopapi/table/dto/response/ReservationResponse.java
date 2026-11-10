package org.group1.coffeeshopapi.table.dto.response;

import org.group1.coffeeshopapi.common.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

// Staff only: carries the guest name and phone, so it never goes in the public table list.
public record ReservationResponse(
        UUID id,
        UUID tableId,
        String tableNumber,
        String customerName,
        String customerPhone,
        Integer partySize,
        LocalDateTime reservedFor,
        LocalDateTime endsAt,
        long durationMinutes,
        ReservationStatus status,
        String note,
        LocalDateTime seatedAt,
        LocalDateTime closedAt,
        String closeReason,
        LocalDateTime createdAt
) {
}
