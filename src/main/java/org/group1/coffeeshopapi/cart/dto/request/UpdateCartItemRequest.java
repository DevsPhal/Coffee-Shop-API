package org.group1.coffeeshopapi.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;

import java.util.List;
import java.util.UUID;

public record UpdateCartItemRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        // Null means "leave unchanged" for these — remove and re-add the item to clear one.
        UUID variantId,
        SugarLevel sugarLevel,
        IceLevel iceLevel,
        MilkType milkType,

        // Null means "leave unchanged"; an empty list clears every extra off this item.
        List<UUID> extraIds
) {
}
