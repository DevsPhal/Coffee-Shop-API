package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.product.entity.Product;

import java.util.UUID;

public final class ProductVariantPolicy {
    private ProductVariantPolicy() {}

    public static void validate(Product product, UUID variantId, SugarLevel sugarLevel,
            IceLevel iceLevel, MilkType milkType) {
        CategoryGroup group = product.getCategory().getCategoryGroup();
        String name = product.getName();

        if (group == CategoryGroup.FRESH_DRINK) {
            return;
        }
        if (group == CategoryGroup.BEVERAGE) {
            if (sugarLevel != null || iceLevel != null || milkType != null) {
                throw new InvalidOperationException(
                        "'" + name + "' only supports a size choice — sugar/ice/milk level don't apply");
            }
            return;
        }
        if (variantId != null || sugarLevel != null || iceLevel != null || milkType != null) {
            throw new InvalidOperationException(
                    "'" + name + "' doesn't support size, sugar, ice, or milk customization");
        }
    }

    public static boolean allowsSizeChoice(Product product) {
        CategoryGroup group = product.getCategory().getCategoryGroup();
        return group == CategoryGroup.FRESH_DRINK || group == CategoryGroup.BEVERAGE;
    }
}
