package org.group1.coffeeshopapi.extra.dto.response;

import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;
import java.util.UUID;

// One extra offered on one product — id is the attachment's own id (used to detach/update it),
// extraId/name/price describe the Extra itself so a menu screen never has to look it up separately.
public record ProductExtraResponse(
        UUID id,
        UUID productId,
        UUID extraId,
        String name,
        BigDecimal price,
        Integer sortOrder,
        Status status
) {
}
