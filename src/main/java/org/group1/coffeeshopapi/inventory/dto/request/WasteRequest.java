package org.group1.coffeeshopapi.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.QuantityUnit;
import org.group1.coffeeshopapi.common.enums.WasteReason;

import java.math.BigDecimal;
import java.util.UUID;

public record WasteRequest(
        @NotNull(message = "Product is required")
        UUID productId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantity must be greater than zero")
        BigDecimal quantity,

        // Blank = BASE: waste is usually a few cans or some grams, not whole cartons.
        QuantityUnit unit,

        @NotNull(message = "Waste reason is required")
        WasteReason wasteReason,

        @Size(max = 255, message = "Note must not exceed 255 characters")
        String note
) {
}
