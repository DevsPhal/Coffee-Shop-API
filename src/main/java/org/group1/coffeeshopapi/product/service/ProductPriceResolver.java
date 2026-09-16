package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;

import java.util.List;

// A product has no price of its own — every price comes from one of its ProductVariant rows.
// When the customer explicitly picked one (only possible where ProductVariantPolicy allows it —
// FRESH_DRINK/BEVERAGE), that's what prices the line. Everywhere else (SNACK, or a BEVERAGE/
// FRESH_DRINK item where the customer just didn't specify one), the product's own single price
// option is what's actually being sold, so it's used automatically. A product configured with
// more than one price option but no way for the customer to choose between them (or zero options
// at all) can't be priced unambiguously, so that's a clear error instead of a silent guess.
public final class ProductPriceResolver {
    private ProductPriceResolver() {}

    public static ProductVariant resolveEffective(Product product, ProductVariant explicit,
            List<ProductVariant> activeOptionsIfNoneExplicit) {
        if (explicit != null) {
            return explicit;
        }
        if (activeOptionsIfNoneExplicit.size() == 1) {
            return activeOptionsIfNoneExplicit.get(0);
        }
        if (activeOptionsIfNoneExplicit.isEmpty()) {
            throw new InvalidOperationException("'" + product.getName() + "' has no price configured yet");
        }
        throw new InvalidOperationException(
                "'" + product.getName() + "' has more than one price option — a variantId is required");
    }
}
