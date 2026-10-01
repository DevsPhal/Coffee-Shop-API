package org.group1.coffeeshopapi.bakong.dto;

import java.math.BigDecimal;

public record BakongTransactionCheckResult(
        boolean paid,
        String transactionHash,
        BigDecimal amount,
        String currency,
        String message,
        boolean failed
) {
    public static BakongTransactionCheckResult paid(String hash, BigDecimal amount, String currency, String message) {
        return new BakongTransactionCheckResult(true, hash, amount, currency, message, false);
    }

    public static BakongTransactionCheckResult notPaid(String message) {
        return new BakongTransactionCheckResult(false, null, null, null, message, false);
    }

    public static BakongTransactionCheckResult failed(String message) {
        return new BakongTransactionCheckResult(false, null, null, null, message, true);
    }
}
