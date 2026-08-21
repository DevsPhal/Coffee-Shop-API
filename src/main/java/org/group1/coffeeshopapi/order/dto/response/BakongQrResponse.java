package org.group1.coffeeshopapi.order.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record BakongQrResponse(
        UUID orderId,
        String qrString,
        String md5Hash,
        BigDecimal amount,
        String currency
) {
}
