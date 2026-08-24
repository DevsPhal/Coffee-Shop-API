package org.group1.coffeeshopapi.order.dto.response;

import org.group1.coffeeshopapi.common.enums.Currency;

import java.math.BigDecimal;
import java.util.UUID;

public record BakongQrResponse(
        UUID orderId,
        String qrString,
        String md5Hash,
        BigDecimal amount,
        Currency currency
) {
}
