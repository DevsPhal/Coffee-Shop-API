package org.group1.coffeeshopapi.bakong.dto;

import java.math.BigDecimal;

/**
 * Outcome of a payment lookup. {@code paid} false has two very different causes that callers
 * must not treat alike: the customer genuinely has not paid yet, or the call never really
 * happened ({@code failed} — expired token, unreachable API). Reporting the second as the first
 * makes a broken integration look like a patient customer.
 */
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

    /** The API answered, and the answer is "no such payment yet". */
    public static BakongTransactionCheckResult notPaid(String message) {
        return new BakongTransactionCheckResult(false, null, null, null, message, false);
    }

    /** The API could not be asked, so nothing at all is known about this payment. */
    public static BakongTransactionCheckResult failed(String message) {
        return new BakongTransactionCheckResult(false, null, null, null, message, true);
    }
}
