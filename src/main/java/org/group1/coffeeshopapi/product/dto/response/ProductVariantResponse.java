package org.group1.coffeeshopapi.product.dto.response;

import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.VariantLabel;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        UUID productId,
        VariantLabel name,
        BigDecimal price,
        BigDecimal finalPrice,
        Integer sortOrder,
        Status status
) {
}
