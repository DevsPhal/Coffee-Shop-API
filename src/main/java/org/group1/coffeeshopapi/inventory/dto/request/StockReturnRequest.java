package org.group1.coffeeshopapi.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.QuantityUnit;

import java.math.BigDecimal;
import java.util.UUID;

public record StockReturnRequest(
        @NotNull(message = "Product is required")
        UUID productId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
        BigDecimal quantity,

        // Blank = BASE: the cans brought back.
        QuantityUnit unit,

        // The order it was sold in; when given, no more can come back than that order took out of stock.
        UUID orderId,

        @Size(max = 255, message = "Note must not exceed 255 characters")
        String note
) {
}
