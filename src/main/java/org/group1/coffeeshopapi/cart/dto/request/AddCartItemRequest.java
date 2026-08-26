package org.group1.coffeeshopapi.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;

import java.util.UUID;

public record AddCartItemRequest(
        @NotNull(message = "Product is required")
        UUID productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        // Variant selection — all optional. When sizeOptionId is set it must belong to productId.
        UUID sizeOptionId,
        SugarLevel sugarLevel,
        IceLevel iceLevel,
        MilkType milkType
) {
}
