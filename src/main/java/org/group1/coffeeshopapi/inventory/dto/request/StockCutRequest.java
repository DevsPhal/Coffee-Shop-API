package org.group1.coffeeshopapi.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.StockStrategy;

import java.math.BigDecimal;
import java.util.UUID;

// Cuts (deducts) stock from a product's oldest (FIFO) or newest (LIFO) batches first.
public record StockCutRequest(
        @NotNull(message = "Product is required")
        UUID productId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @NotNull(message = "Strategy is required")
        StockStrategy strategy,

        String note
) {
}