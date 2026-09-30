package org.group1.coffeeshopapi.payway.dto;

import java.math.BigDecimal;

// paid is only true once PayWay reports the transaction APPROVED.
public record PaywayTransactionCheckResult(
        boolean paid,
        String approvalCode,
        BigDecimal amount,
        String message
) {
    public static PaywayTransactionCheckResult paid(String approvalCode, BigDecimal amount) {
        return new PaywayTransactionCheckResult(true, approvalCode, amount, null);
    }

    public static PaywayTransactionCheckResult notPaid(String message) {
        return new PaywayTransactionCheckResult(false, null, null, message);
    }
}
