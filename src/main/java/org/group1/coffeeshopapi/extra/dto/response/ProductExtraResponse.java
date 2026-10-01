package org.group1.coffeeshopapi.extra.dto.response;

import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductExtraResponse(
        UUID id,
        UUID productId,
        UUID extraId,
        String name,
        BigDecimal price,
        Integer sortOrder,
        Status status,

        BigDecimal quantityOnHand,

        String imageUrl
) {
}
