package org.group1.coffeeshopapi.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.QuantityUnit;

import java.math.BigDecimal;
import java.util.UUID;

// A physical count: stock is set to what is really on the shelf, and the difference is recorded as an adjustment.
public record StockCountRequest(
        @NotNull(message = "Product is required")
        UUID productId,

        @NotNull(message = "Counted quantity is required")
        @DecimalMin(value = "0.0", message = "Counted quantity must not be negative")
        BigDecimal countedQuantity,

        // Blank = BASE.
        QuantityUnit unit,

        @Size(max = 255, message = "Note must not exceed 255 characters")
        String note
) {
}
