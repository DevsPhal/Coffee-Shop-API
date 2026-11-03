package org.group1.coffeeshopapi.recipe.service;

import org.group1.coffeeshopapi.recipe.dto.request.SetRecipeRequest;
import org.group1.coffeeshopapi.recipe.dto.response.RecipeResponse;

import java.util.List;
import java.util.UUID;

public interface RecipeService {
    // One entry per size of the product, with an empty item list where no recipe is set.
    List<RecipeResponse> getRecipes(UUID productId);

    RecipeResponse setRecipe(UUID productId, UUID variantId, SetRecipeRequest request);
}
