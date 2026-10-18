package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductVariantPolicyTest {

    @Test
    void snackAcceptsItsOwnPieceVariant() {
        Product pancake = product("Pancake", CategoryGroup.SNACK);

        assertThatCode(() -> ProductVariantPolicy.validate(pancake, variant(VariantLabel.PIECE), null, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void snackWithNoVariantNamedIsAccepted() {
        Product pancake = product("Pancake", CategoryGroup.SNACK);

        assertThatCode(() -> ProductVariantPolicy.validate(pancake, null, null, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void snackRejectsASizeVariant() {
        Product pancake = product("Pancake", CategoryGroup.SNACK);

        assertThatThrownBy(() -> ProductVariantPolicy.validate(pancake, variant(VariantLabel.MEDIUM), null, null, null))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Pancake")
                .hasMessageContaining("doesn't support size");
    }

    @Test
    void snackRejectsDrinkOptionsEvenWithItsPieceVariant() {
        Product pancake = product("Pancake", CategoryGroup.SNACK);

        assertThatThrownBy(() -> ProductVariantPolicy.validate(pancake, variant(VariantLabel.PIECE),
                SugarLevel.NORMAL, null, null))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("doesn't support size, sugar, ice, or milk");
    }

    @Test
    void productInAnInternalCategoryAcceptsItsPieceVariant() {
        Product product = product("Staff Meal", null);

        assertThatCode(() -> ProductVariantPolicy.validate(product, variant(VariantLabel.PIECE), null, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void beverageStillRejectsSugarLevel() {
        Product cola = product("Cola", CategoryGroup.BEVERAGE);

        assertThatThrownBy(() -> ProductVariantPolicy.validate(cola, variant(VariantLabel.MEDIUM),
                SugarLevel.LESS, null, null))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("only supports a size choice");
    }

    private Product product(String name, CategoryGroup group) {
        Category category = new Category();
        category.setCategoryGroup(group);
        Product product = new Product();
        product.setName(name);
        product.setCategory(category);
        return product;
    }

    private ProductVariant variant(VariantLabel label) {
        ProductVariant variant = new ProductVariant();
        variant.setName(label);
        return variant;
    }
}
