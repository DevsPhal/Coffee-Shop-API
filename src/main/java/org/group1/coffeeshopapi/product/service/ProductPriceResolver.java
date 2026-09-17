package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;

import java.util.List;

// Picks which variant prices a line: the explicit one if given, or the product's single variant
// if it only has one. Errors if the product has none or more than one and none was picked.
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
