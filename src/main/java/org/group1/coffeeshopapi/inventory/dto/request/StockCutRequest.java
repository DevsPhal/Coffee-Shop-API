package org.group1.coffeeshopapi.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.QuantityUnit;
import org.group1.coffeeshopapi.common.enums.StockStrategy;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.Size;

public record StockCutRequest(
        @NotNull(message = "Product is required")
        UUID productId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
        BigDecimal quantity,

        // Blank = PURCHASE (whole stock units), as before; BASE cuts single cans.
        QuantityUnit unit,

        @NotNull(message = "Strategy is required")
        StockStrategy strategy,

        @Size(max = 255, message = "Note must not exceed 255 characters")
        String note
) {
}
