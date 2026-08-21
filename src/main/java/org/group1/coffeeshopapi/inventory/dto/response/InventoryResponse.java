package org.group1.coffeeshopapi.inventory.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryResponse(
        UUID productId,
        String productName,
        String unit,
        BigDecimal quantityOnHand,
        BigDecimal reorderLevel
) {
}