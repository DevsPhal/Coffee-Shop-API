package org.group1.coffeeshopapi.product.dto.response;

import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.VariantLabel;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        UUID productId,
        VariantLabel name,
        String sku,
        BigDecimal price,
        BigDecimal finalPrice,
        Integer unitsPerSale,
        Integer sortOrder,
        Status status,
        // How many can be sold right now; null where it wasn't worked out.
        Integer availableQuantity,
        // Sold from a recipe's ingredients rather than the product's own stock.
        boolean recipeBased
) {
}
