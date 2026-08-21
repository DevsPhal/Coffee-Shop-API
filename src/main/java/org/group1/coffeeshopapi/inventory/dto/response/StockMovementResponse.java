package org.group1.coffeeshopapi.inventory.dto.response;

import org.group1.coffeeshopapi.common.enums.StockMovementType;
import org.group1.coffeeshopapi.common.enums.StockStrategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        UUID productId,
        String productName,
        StockMovementType type,
        StockStrategy strategy,
        BigDecimal quantity,
        String note,
        UUID performedBy,
        LocalDateTime createdAt
) {
}
