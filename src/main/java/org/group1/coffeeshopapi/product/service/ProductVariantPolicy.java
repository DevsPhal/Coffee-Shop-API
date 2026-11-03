package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public final class ProductVariantPolicy {
    private ProductVariantPolicy() {}

    public static void validate(Product product, ProductVariant variant, SugarLevel sugarLevel,
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
        // Snacks still carry their single PIECE price option; only a size or drink option is a customization.
        boolean sizeChosen = variant != null && !allowedVariants(group).contains(variant.getName());
        if (sizeChosen || sugarLevel != null || iceLevel != null || milkType != null) {
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
        if (group == CategoryGroup.INGREDIENT) {
            return EnumSet.noneOf(VariantLabel.class);
        }
        Set<VariantLabel> allowed = allowsSizeChoice(group)
                ? EnumSet.of(VariantLabel.MEDIUM, VariantLabel.LARGE)
                : EnumSet.of(VariantLabel.PIECE);
        // Fresh drinks are made by the cup; packaged goods can also be sold by the pack, box or carton.
        if (group != CategoryGroup.FRESH_DRINK) {
            allowed.addAll(EnumSet.of(VariantLabel.PACK, VariantLabel.BOX, VariantLabel.CARTON));
        }
        return allowed;
    }

    // A size sells one base unit. A pack variant named after the stock unit sells a whole one (a carton of 24 cans);
    // any other pack size has to be given, since nothing says how many cans are in it.
    public static int defaultUnitsPerSale(Product product, VariantLabel label) {
        if (!label.isMultiUnit()) {
            return 1;
        }
        StockUnit stockUnit = product.getStockUnit();
        BigDecimal unitsPerStock = product.getUnitsPerStock();
        if (stockUnit == null || !label.name().equals(stockUnit.name())) {
            throw new InvalidOperationException("Enter how many " + product.getSellUnit() + " one " + label + " of '"
                    + product.getName() + "' holds — it is only filled in automatically for the " + stockUnit
                    + " it is bought in");
        }
        if (unitsPerStock == null || unitsPerStock.signum() <= 0 || unitsPerStock.stripTrailingZeros().scale() > 0) {
            throw new InvalidOperationException("'" + product.getName() + "' needs a whole number of units per stock "
                    + "before it can be sold by the " + label);
        }
        return unitsPerStock.intValueExact();
    }

    public static VariantLabel defaultVariant(CategoryGroup group) {
        return allowsSizeChoice(group) ? VariantLabel.MEDIUM : VariantLabel.PIECE;
    }

    public static void requireAllowed(CategoryGroup group, VariantLabel label, String productName) {
        if (group == CategoryGroup.INGREDIENT) {
            throw new InvalidOperationException("'" + productName + "' is an ingredient: it is used up by recipes and "
                    + "has no price options of its own");
        }
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
