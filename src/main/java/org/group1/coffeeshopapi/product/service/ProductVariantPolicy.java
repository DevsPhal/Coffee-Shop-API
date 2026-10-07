package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.product.entity.Product;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
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
        return allowsSizeChoice(product.getCategory().getCategoryGroup());
    }

    public static boolean allowsSizeChoice(CategoryGroup group) {
        return group == CategoryGroup.FRESH_DRINK || group == CategoryGroup.BEVERAGE;
    }

    public static Set<VariantLabel> allowedVariants(CategoryGroup group) {
        return allowsSizeChoice(group)
                ? EnumSet.of(VariantLabel.MEDIUM, VariantLabel.LARGE)
                : EnumSet.of(VariantLabel.PIECE);
    }

    public static VariantLabel defaultVariant(CategoryGroup group) {
        return allowsSizeChoice(group) ? VariantLabel.MEDIUM : VariantLabel.PIECE;
    }

    public static void requireAllowed(CategoryGroup group, VariantLabel label, String productName) {
        Set<VariantLabel> allowed = allowedVariants(group);
        if (!allowed.contains(label)) {
            throw new InvalidOperationException("'" + productName + "' is in a " + describe(group)
                    + " category, which only allows variants " + allowed + " — " + label + " is not allowed");
        }
    }

    public static void requireAllAllowed(CategoryGroup group, Collection<VariantLabel> labels, String productName) {
        for (VariantLabel label : labels) {
            requireAllowed(group, label, productName);
        }
    }

    public static String describe(CategoryGroup group) {
        return group == null ? "general" : group.name().toLowerCase().replace('_', ' ');
    }
}
