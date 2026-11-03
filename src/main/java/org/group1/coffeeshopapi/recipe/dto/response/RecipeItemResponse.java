package org.group1.coffeeshopapi.recipe.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record RecipeItemResponse(
        UUID ingredientId,
        String ingredientName,
        // The ingredient's base unit that quantity, unitCost and onHand are in.
        String unit,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal lineCost,
        BigDecimal onHand
) {
}
