package org.group1.coffeeshopapi.bakong.dto;

import org.group1.coffeeshopapi.common.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BakongQrResult(
        String qrString,
        String md5Hash,
        Currency currency,
        BigDecimal amount,
        LocalDateTime expiresAt
) {
}
