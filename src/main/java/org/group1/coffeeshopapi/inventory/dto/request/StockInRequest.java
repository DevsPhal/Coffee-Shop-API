package org.group1.coffeeshopapi.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.QuantityUnit;

import java.math.BigDecimal;
import java.util.UUID;

public record StockInRequest(
        @NotNull(message = "Product is required")
        UUID productId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
        BigDecimal quantity,

        // Blank = PURCHASE: quantity and unit cost are per stock unit (carton), and stock grows by the cans in it.
        QuantityUnit unit,

        @NotNull(message = "Unit cost is required")
        @DecimalMin(value = "0.0", message = "Unit cost must not be negative")
        BigDecimal unitCost,

        String note
) {
}
