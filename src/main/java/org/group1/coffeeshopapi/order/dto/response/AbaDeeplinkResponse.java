package org.group1.coffeeshopapi.order.dto.response;

import java.util.UUID;

// An abamobilebank:// link that opens ABA Mobile on this order's payment.
public record AbaDeeplinkResponse(
        UUID orderId,
        String deeplink
) {
}
