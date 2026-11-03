package org.group1.coffeeshopapi.inventory.dto.response;

import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.StockMovementReason;
import org.group1.coffeeshopapi.common.enums.StockMovementType;
import org.group1.coffeeshopapi.common.enums.StockStrategy;
import org.group1.coffeeshopapi.common.enums.WasteReason;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementResponse(
        UUID id,
        UUID productId,
        String productName,
        StockMovementType type,
        StockMovementReason reason,
        WasteReason wasteReason,
        StockStrategy strategy,
        // In `unit`, the product's base unit, as is balanceAfter.
        BigDecimal quantity,
        String unit,
        BigDecimal balanceAfter,
        BigDecimal totalCost,
        UUID orderId,
        String note,
        UUID performedBy,
        String performedByName,
        Role performedByRole,
        LocalDateTime createdAt
) {
}
