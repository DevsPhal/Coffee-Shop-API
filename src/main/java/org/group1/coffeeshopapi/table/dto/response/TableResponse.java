package org.group1.coffeeshopapi.table.dto.response;

import org.group1.coffeeshopapi.common.enums.TableSize;
import org.group1.coffeeshopapi.common.enums.TableStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TableResponse(
        UUID id,
        String tableNumber,
        TableSize size,
        Integer capacity,
        Integer guestCount,
        TableStatus status,
        String scanUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
