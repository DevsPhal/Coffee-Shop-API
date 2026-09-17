package org.group1.coffeeshopapi.extra.dto.response;

import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;
import java.util.UUID;

public record ExtraResponse(
        UUID id,
        String name,
        BigDecimal price,
        Status status,

        // Null means untracked (always available).
        BigDecimal quantityOnHand
) {
}
