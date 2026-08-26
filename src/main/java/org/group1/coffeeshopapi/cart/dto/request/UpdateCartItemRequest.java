package org.group1.coffeeshopapi.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;

import java.util.UUID;

public record UpdateCartItemRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        // Variant selection — null means "leave unchanged"; there is no way to clear a
        // previously-set size/sugar/ice/milk back to "unspecified" through this endpoint alone
        // (remove and re-add the item instead).
        UUID sizeOptionId,
        SugarLevel sugarLevel,
        IceLevel iceLevel,
        MilkType milkType
) {
}
