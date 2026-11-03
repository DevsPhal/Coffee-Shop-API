package org.group1.coffeeshopapi.recipe.dto.response;

import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.VariantLabel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// One size of a drink and what goes into it. Costs are at the average cost of the ingredients on hand.
public record RecipeResponse(
        UUID productId,
        UUID variantId,
        VariantLabel variantName,
        Status variantStatus,
        BigDecimal finalPrice,
        List<RecipeItemResponse> items,
        BigDecimal cost,
        BigDecimal margin,
        // Margin as a percentage of the selling price; null when the price is 0.
        BigDecimal marginPercent,
        int availableQuantity
) {
}
