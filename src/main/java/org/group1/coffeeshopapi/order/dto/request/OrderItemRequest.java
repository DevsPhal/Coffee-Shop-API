package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;

import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;

public record OrderItemRequest(
        @NotNull(message = "Product is required")
        UUID productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = ValidationPatterns.MAX_ITEM_QUANTITY, message = "Quantity must be at most 999")
        Integer quantity,

        UUID variantId,
        @Size(max = 20, message = "Size name must not exceed 20 characters")
        String variantName,
        SugarLevel sugarLevel,
        IceLevel iceLevel,
        MilkType milkType,

        List<UUID> extraIds
) {
}
