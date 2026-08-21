package org.group1.coffeeshopapi.bakong.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Raw shape of a POST /v1/check_transaction_by_md5 response from the Bakong Open API.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BakongCheckTransactionResponse(
        int responseCode,
        String responseMessage,
        Integer errorCode,
        BakongTransactionData data
) {
}
