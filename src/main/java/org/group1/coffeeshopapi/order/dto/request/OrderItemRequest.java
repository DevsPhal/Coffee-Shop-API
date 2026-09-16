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

        // Variant selection — all optional. Pick a size by whichever's actually on hand: its id
        // (variantId — a UUID, what a client that already fetched the product's options has)
        // or its name (variantName, e.g. "Medium" — what a walk-up POS screen's button actually
        // shows, not a UUID). variantId wins if both are somehow given. When variantId is set
        // it must belong to productId; variantName is matched case-insensitively.
        UUID variantId,
        String variantName,
        SugarLevel sugarLevel,
        IceLevel iceLevel,
        MilkType milkType,

        // Extras (e.g. Pearl) to add — each must be offered (and active) on productId, see
        // ProductExtra. Null/empty means none.
        List<UUID> extraIds
) {
}
