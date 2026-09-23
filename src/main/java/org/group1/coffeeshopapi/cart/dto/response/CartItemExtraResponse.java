package org.group1.coffeeshopapi.cart.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemExtraResponse(
        UUID extraId,
        String name,
        BigDecimal price,
        String imageUrl
) {
}
