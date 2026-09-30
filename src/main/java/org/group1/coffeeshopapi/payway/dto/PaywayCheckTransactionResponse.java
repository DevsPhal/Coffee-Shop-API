package org.group1.coffeeshopapi.payway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

// Raw PayWay check-transaction-2 response.
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaywayCheckTransactionResponse(
        PaywayStatus status,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("payment_status_code") Integer paymentStatusCode,
            @JsonProperty("payment_status") String paymentStatus,
            @JsonProperty("payment_amount") BigDecimal paymentAmount,
            @JsonProperty("apv") String approvalCode
    ) {
    }
}
