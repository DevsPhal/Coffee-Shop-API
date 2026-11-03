package org.group1.coffeeshopapi.recipe.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// Replaces the whole recipe of one size. An empty list removes it, so the size is sold from its own stock again.
public record SetRecipeRequest(
        @NotNull(message = "Items are required")
        List<@Valid RecipeItemRequest> items
) {
}
