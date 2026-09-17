package org.group1.coffeeshopapi.bakong.dto;

import org.group1.coffeeshopapi.common.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// A generated KHQR. expiresAt matches the expiration encoded into the QR itself.
public record BakongQrResult(
        String qrString,
        String md5Hash,
        Currency currency,
        BigDecimal amount,
        LocalDateTime expiresAt
) {
}
