package org.group1.coffeeshopapi.inventory.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockExpenseResponse(
        UUID id,
        UUID productId,
        String productName,
        UUID stockMovementId,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal amount,
        LocalDate expenseDate,
        LocalDateTime createdAt
) {
}
