package org.group1.coffeeshopapi.bakong.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BakongTransactionData(
        String hash,
        String fromAccountId,
        String toAccountId,
        String currency,
        String amount,
        String description
) {
}
