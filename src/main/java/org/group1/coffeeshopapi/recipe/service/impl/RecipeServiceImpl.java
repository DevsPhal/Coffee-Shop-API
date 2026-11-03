package org.group1.coffeeshopapi.recipe.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.QuantityUnit;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.inventory.service.VariantStock;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.realtime.ResourceChangePublisher;
import org.group1.coffeeshopapi.realtime.ChangeType;
import org.group1.coffeeshopapi.realtime.ResourceType;
import org.group1.coffeeshopapi.recipe.dto.request.RecipeItemRequest;
import org.group1.coffeeshopapi.recipe.dto.request.SetRecipeRequest;
import org.group1.coffeeshopapi.recipe.dto.response.RecipeItemResponse;
import org.group1.coffeeshopapi.recipe.dto.response.RecipeResponse;
import org.group1.coffeeshopapi.recipe.entity.RecipeItem;
import org.group1.coffeeshopapi.recipe.repository.RecipeItemRepository;
import org.group1.coffeeshopapi.recipe.service.RecipeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeServiceImpl implements RecipeService {

    private static final int COST_SCALE = 4;

    private final RecipeItemRepository recipeItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final ResourceChangePublisher resourceChangePublisher;

    @Override
    public List<RecipeResponse> getRecipes(UUID productId) {
        Product product = findProduct(productId);
        List<ProductVariant> variants = variantRepository.findByProductIdOrderBySortOrderAscNameAsc(product.getId());
        Map<UUID, List<RecipeItem>> recipes = recipeItemRepository.findByProductId(product.getId()).stream()
                .collect(Collectors.groupingBy(item -> item.getVariant().getId()));
        return toResponses(variants, recipes);
    }

    @Override
    @Transactional
    public RecipeResponse setRecipe(UUID productId, UUID variantId, SetRecipeRequest request) {
        Product product = findProduct(productId);
        if (product.isIngredient()) {
            throw new InvalidOperationException("'" + product.getName() + "' is an ingredient; recipes belong to "
                    + "the drinks that use it");
        }
        ProductVariant variant = variantRepository.findByIdAndProductId(variantId, product.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        Set<UUID> ingredientIds = new HashSet<>();
        for (RecipeItemRequest item : request.items()) {
            if (!ingredientIds.add(item.ingredientId())) {
                throw new InvalidOperationException("Each ingredient can only be listed once in a recipe");
            }
        }
        Map<UUID, Product> ingredients = productRepository.findAllById(ingredientIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        for (UUID ingredientId : ingredientIds) {
            Product ingredient = ingredients.get(ingredientId);
            if (ingredient == null) {
                throw new ResourceNotFoundException("Ingredient not found: " + ingredientId);
            }
            if (!ingredient.isIngredient()) {
                throw new InvalidOperationException("'" + ingredient.getName() + "' isn't an ingredient — move it to "
                        + "an ingredient category before using it in a recipe");
            }
        }

        recipeItemRepository.deleteByVariantId(variant.getId());
        List<RecipeItem> saved = recipeItemRepository.saveAll(request.items().stream()
                .map(item -> {
                    RecipeItem recipeItem = new RecipeItem();
                    recipeItem.setVariant(variant);
                    recipeItem.setIngredient(ingredients.get(item.ingredientId()));
                    recipeItem.setQuantity(item.quantity());
                    return recipeItem;
                })
                .toList());
        // What the menu shows as available changes with the recipe.
        resourceChangePublisher.record(ResourceType.PRODUCT, product.getId(), ChangeType.UPDATED);

        return toResponses(List.of(variant), Map.of(variant.getId(), saved)).get(0);
    }

    private List<RecipeResponse> toResponses(List<ProductVariant> variants, Map<UUID, List<RecipeItem>> recipes) {
        Set<UUID> ingredientIds = recipes.values().stream().flatMap(List::stream)
                .map(item -> item.getIngredient().getId()).collect(Collectors.toSet());
        Map<UUID, BigDecimal> unitCosts = inventoryService.averageUnitCosts(ingredientIds);
        Map<UUID, BigDecimal> onHand = inventoryRepository.findByProductIdIn(ingredientIds).stream()
                .collect(Collectors.toMap(inventory -> inventory.getProduct().getId(), Inventory::getQuantityOnHand));
        Map<UUID, VariantStock> stock = inventoryService.variantStock(variants);
        LocalDateTime now = LocalDateTime.now();

        return variants.stream().map(variant -> {
            List<RecipeItemResponse> items = recipes.getOrDefault(variant.getId(), List.of()).stream()
                    .map(item -> {
                        Product ingredient = item.getIngredient();
                        BigDecimal unitCost = unitCosts.getOrDefault(ingredient.getId(), BigDecimal.ZERO);
                        return new RecipeItemResponse(ingredient.getId(), ingredient.getName(),
                                ingredient.unitName(QuantityUnit.BASE), item.getQuantity(), unitCost,
                                item.getQuantity().multiply(unitCost).setScale(COST_SCALE, RoundingMode.HALF_UP),
                                onHand.getOrDefault(ingredient.getId(), BigDecimal.ZERO));
                    })
                    .toList();
            BigDecimal cost = items.stream().map(RecipeItemResponse::lineCost).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal price = variant.getProduct().getFinalPrice(variant.getPrice(), now);
            BigDecimal margin = price.subtract(cost);
            BigDecimal marginPercent = price.signum() > 0
                    ? margin.multiply(BigDecimal.valueOf(100)).divide(price, 2, RoundingMode.HALF_UP)
                    : null;
            VariantStock variantStock = stock.get(variant.getId());
            return new RecipeResponse(variant.getProduct().getId(), variant.getId(), variant.getName(),
                    variant.getStatus(), price, items, cost, margin, marginPercent,
                    variantStock != null ? variantStock.available() : 0);
        }).toList();
    }

    private Product findProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }
}
