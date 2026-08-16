package org.group1.coffeeshopapi.payments.dto.response;

public record BakongResponse(
        int responseCode,
        String responseMessage,
        Integer errorCode,
        BakongTransactionData data
) {
}
