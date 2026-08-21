package org.group1.coffeeshopapi.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

// Receives a new stock batch ("lot") into inventory for a product.
public record StockInRequest(
        @NotNull(message = "Product is required")
        UUID productId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @NotNull(message = "Unit cost is required")
        @DecimalMin(value = "0.0", message = "Unit cost must not be negative")
        BigDecimal unitCost,

        String note
) {
}
