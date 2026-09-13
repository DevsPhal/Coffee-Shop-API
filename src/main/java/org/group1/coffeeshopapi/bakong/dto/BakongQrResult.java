package org.group1.coffeeshopapi.bakong.dto;

import org.group1.coffeeshopapi.common.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A generated KHQR. {@code expiresAt} mirrors the expiration stamped into the QR itself (EMV
 * tag 99), so a payment screen can count down to the moment the code actually stops working
 * rather than to a timer of its own invention.
 */
public record BakongQrResult(
        String qrString,
        String md5Hash,
        Currency currency,
        BigDecimal amount,
        LocalDateTime expiresAt
) {
}
