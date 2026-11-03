package org.group1.coffeeshopapi.recipe.service.impl;

import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.realtime.ResourceChangePublisher;
import org.group1.coffeeshopapi.recipe.dto.request.RecipeItemRequest;
import org.group1.coffeeshopapi.recipe.dto.request.SetRecipeRequest;
import org.group1.coffeeshopapi.recipe.repository.RecipeItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceImplTest {

    @Mock private RecipeItemRepository recipeItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private InventoryService inventoryService;
    @Mock private ResourceChangePublisher resourceChangePublisher;
    @InjectMocks private RecipeServiceImpl service;

    @Test
    void onlyIngredientsCanGoIntoARecipe() {
        Product latte = product("Iced Latte", CategoryGroup.FRESH_DRINK);
        ProductVariant medium = variantOf(latte);
        Product coke = product("Coke", CategoryGroup.BEVERAGE);
        when(productRepository.findAllById(any())).thenReturn(List.of(coke));

        assertThatThrownBy(() -> service.setRecipe(latte.getId(), medium.getId(), new SetRecipeRequest(List.of(
                new RecipeItemRequest(coke.getId(), BigDecimal.ONE)))))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("isn't an ingredient");
        verify(recipeItemRepository, never()).deleteByVariantId(any());
    }

    @Test
    void anIngredientIsListedOnlyOnce() {
        Product latte = product("Iced Latte", CategoryGroup.FRESH_DRINK);
        ProductVariant medium = variantOf(latte);
        UUID milk = UUID.randomUUID();

        assertThatThrownBy(() -> service.setRecipe(latte.getId(), medium.getId(), new SetRecipeRequest(List.of(
                new RecipeItemRequest(milk, BigDecimal.TEN), new RecipeItemRequest(milk, BigDecimal.ONE)))))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("only be listed once");
    }

    @Test
    void anIngredientHasNoRecipeOfItsOwn() {
        Product milk = product("Fresh Milk", CategoryGroup.INGREDIENT);

        assertThatThrownBy(() -> service.setRecipe(milk.getId(), UUID.randomUUID(), new SetRecipeRequest(List.of())))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("is an ingredient");
    }

    private Product product(String name, CategoryGroup group) {
        Category category = new Category();
        category.setCategoryGroup(group);
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setCategory(category);
        org.mockito.Mockito.lenient().when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        return product;
    }

    private ProductVariant variantOf(Product product) {
        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setProduct(product);
        when(variantRepository.findByIdAndProductId(variant.getId(), product.getId())).thenReturn(Optional.of(variant));
        return variant;
    }
}
