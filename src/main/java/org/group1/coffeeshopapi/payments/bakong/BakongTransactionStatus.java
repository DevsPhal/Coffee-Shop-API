package org.group1.coffeeshopapi.payments.bakong;

public record BakongTransactionStatus(boolean paid, String transactionHash, String message) {
}
