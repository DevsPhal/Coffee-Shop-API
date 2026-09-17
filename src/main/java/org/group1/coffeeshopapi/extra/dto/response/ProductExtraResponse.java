package org.group1.coffeeshopapi.extra.dto.response;

import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;
import java.util.UUID;

// One extra offered on one product. id is the attachment's id; extraId/name/price describe the
// Extra itself.
public record ProductExtraResponse(
        UUID id,
        UUID productId,
        UUID extraId,
        String name,
        BigDecimal price,
        Integer sortOrder,
        Status status,

        // Null means untracked (always available). Hidden from customers once it hits zero.
        BigDecimal quantityOnHand
) {
}
