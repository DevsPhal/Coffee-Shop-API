package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.product.entity.Product;

import java.util.UUID;

// Which cart/order item customizations a product accepts, driven by its category's top-level
// menu grouping: FRESH_DRINK (iced/hot coffee, tea) takes size, sugar, ice, and milk since it's
// made to order; BEVERAGE (beer, water, soda, ...) only takes a size choice — a canned drink has
// no sugar/ice/milk to adjust; SNACK (noodle, ...) and anything with no group at all (e.g. the
// internal "Material" category) take none of it. Shared by CartServiceImpl (fast feedback while
// building the cart) and OrderServiceImpl.buildOrder (the authoritative check — it's also the
// only validation a barista's direct walk-up order goes through, since that path skips the cart).
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
        // SNACK, or no group at all.
        if (variantId != null || sugarLevel != null || iceLevel != null || milkType != null) {
            throw new InvalidOperationException(
                    "'" + name + "' doesn't support size, sugar, ice, or milk customization");
        }
    }

    /**
     * Whether a caller may explicitly pick a variant for this product — true for
     * FRESH_DRINK/BEVERAGE, false for SNACK/no-group (which still resolve a variant
     * internally for pricing even though nothing here let the caller choose one — see
     * ProductPriceResolver). Use this to tell "the caller picked this" apart from "this was
     * auto-resolved" when re-deriving a request from already-resolved state, e.g. turning a
     * Cart's items back into an order at checkout — passing an auto-resolved id straight back
     * into {@link #validate} would otherwise look like an explicit (and, for SNACK, forbidden)
     * choice.
     */
    public static boolean allowsSizeChoice(Product product) {
        CategoryGroup group = product.getCategory().getCategoryGroup();
        return group == CategoryGroup.FRESH_DRINK || group == CategoryGroup.BEVERAGE;
    }
}
