package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;

import java.util.List;
import java.util.UUID;

public record OrderItemRequest(
        @NotNull(message = "Product is required")
        UUID productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        // Pick a size by id or by name (e.g. "Medium", matched case-insensitively). Both optional;
        // variantId wins if both are given.
        UUID variantId,
        String variantName,
        SugarLevel sugarLevel,
        IceLevel iceLevel,
        MilkType milkType,

        // Extras (e.g. Pearl) to add. Null/empty means none.
        List<UUID> extraIds
) {
}
