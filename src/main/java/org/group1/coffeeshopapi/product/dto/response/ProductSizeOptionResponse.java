package org.group1.coffeeshopapi.product.dto.response;

import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSizeOptionResponse(
        UUID id,
        UUID productId,
        String name,
        BigDecimal priceDelta,
        Integer sortOrder,
        Status status
) {
}
