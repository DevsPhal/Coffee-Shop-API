package org.group1.coffeeshopapi.recipe.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RecipeItemRequest(
        @NotNull(message = "Ingredient is required")
        UUID ingredientId,

        // In the ingredient's base unit, e.g. 18 (GRAM of beans) or 150 (MILLILITER of milk).
        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "1", message = "Quantity must be at least 1")
        @Digits(integer = 9, fraction = 0, message = "Quantity must be a whole number of the ingredient's unit")
        BigDecimal quantity
) {
}
