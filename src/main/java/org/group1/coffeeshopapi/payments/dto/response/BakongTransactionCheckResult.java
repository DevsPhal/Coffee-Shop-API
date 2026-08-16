package org.group1.coffeeshopapi.payments.dto.response;

public record BakongTransactionCheckResult(boolean paid, String transactionHash, String message) {
}
