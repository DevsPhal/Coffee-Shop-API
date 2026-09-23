package org.group1.coffeeshopapi.order.dto.response;

import java.util.UUID;

// A link that opens the customer's banking app to pay this order directly.
public record BakongDeeplinkResponse(
        UUID orderId,
        String deeplink
) {
}
