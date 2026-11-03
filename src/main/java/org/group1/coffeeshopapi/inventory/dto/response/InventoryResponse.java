package org.group1.coffeeshopapi.inventory.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryResponse(
        UUID productId,
        String productName,
        // The base unit (the product's sell unit) quantityOnHand and reorderLevel are counted in, e.g. CAN.
        String unit,
        BigDecimal quantityOnHand,
        BigDecimal reorderLevel,
        // The unit stock is bought in and how many base units it holds, e.g. CARTON of 24.
        String stockUnit,
        BigDecimal unitsPerStock,
        // Used up by recipes rather than sold.
        boolean ingredient
) {
}
