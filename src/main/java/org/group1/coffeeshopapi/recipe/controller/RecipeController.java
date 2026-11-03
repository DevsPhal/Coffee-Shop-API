package org.group1.coffeeshopapi.recipe.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.recipe.dto.request.SetRecipeRequest;
import org.group1.coffeeshopapi.recipe.dto.response.RecipeResponse;
import org.group1.coffeeshopapi.recipe.service.RecipeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products/{productId}")
@RequiredArgsConstructor
@Tag(name = "Recipes", description = "Admin and Barista: view what each size of a drink is made of and what it costs. "
        + "Admin only: set the recipe of a size. A size with a recipe uses up its ingredients when sold.")
@SecurityRequirement(name = "bearerAuth")
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping("/recipes")
    public ApiResponse<List<RecipeResponse>> list(@PathVariable UUID productId) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, recipeService.getRecipes(productId));
    }

    @PutMapping("/variants/{variantId}/recipe")
    public ApiResponse<RecipeResponse> set(@PathVariable UUID productId, @PathVariable UUID variantId,
            @Valid @RequestBody SetRecipeRequest request) {
        return ApiResponse.of(HttpStatus.OK, "Recipe saved.", recipeService.setRecipe(productId, variantId, request));
    }
}
