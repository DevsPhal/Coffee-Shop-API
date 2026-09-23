package org.group1.coffeeshopapi.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemExtraResponse(
        UUID extraId,
        String name,
        BigDecimal price,
        String imageUrl
) {
}
