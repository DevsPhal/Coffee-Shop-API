package org.group1.coffeeshopapi.inventory.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

// One product or ingredient over a report period. Quantities are in `unit` (its base unit), costs in USD.
public record InventoryReportRow(
        UUID productId,
        String productName,
        String categoryName,
        boolean ingredient,
        String unit,
        BigDecimal purchased,
        BigDecimal sold,
        BigDecimal returned,
        BigDecimal wasted,
        BigDecimal adjustedIn,
        BigDecimal adjustedOut,
        BigDecimal purchaseCost,
        BigDecimal salesCost,
        BigDecimal wasteCost,
        // Now, not at the end of the period.
        BigDecimal onHand,
        BigDecimal reorderLevel,
        BigDecimal stockValue
) {
}
