package org.group1.coffeeshopapi.bakong.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BakongCheckTransactionResponse(
        int responseCode,
        String responseMessage,
        Integer errorCode,
        BakongTransactionData data
) {
}
