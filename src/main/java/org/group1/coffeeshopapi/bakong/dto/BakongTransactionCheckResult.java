package org.group1.coffeeshopapi.bakong.dto;

import java.math.BigDecimal;

public record BakongTransactionCheckResult(
        boolean paid,
        String transactionHash,
        BigDecimal amount,
        String currency,
        String message
) {
}
