package org.group1.coffeeshopapi.bakong.dto;

import org.group1.coffeeshopapi.common.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A generated KHQR. {@code amount} is in {@code currency}; for KHR it is the riel amount rounded to the nearest
 * 100 at {@code exchangeRate} (KHR per USD), which is null for a USD QR.
 */
public record BakongQrResult(
        String qrString,
        String md5Hash,
        Currency currency,
        BigDecimal amount,
        LocalDateTime expiresAt,
        BigDecimal exchangeRate
) {
}
