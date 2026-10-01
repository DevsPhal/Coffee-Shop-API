package org.group1.coffeeshopapi.order.dto.response;

import java.util.UUID;

public record BakongDeeplinkResponse(
        UUID orderId,
        String deeplink
) {
}
