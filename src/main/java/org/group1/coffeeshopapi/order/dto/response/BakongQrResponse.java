package org.group1.coffeeshopapi.order.dto.response;

import org.group1.coffeeshopapi.common.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// amount is what the QR actually encodes (e.g. whole riel for KHR) — show this, not the order
// total. expiresInSeconds is the same deadline as expiresAt, but as a duration a phone in any
// timezone can safely count down from.
public record BakongQrResponse(
        UUID orderId,
        String qrString,
        String md5Hash,
        BigDecimal amount,
        Currency currency,
        LocalDateTime expiresAt,
        long expiresInSeconds
) {
}
