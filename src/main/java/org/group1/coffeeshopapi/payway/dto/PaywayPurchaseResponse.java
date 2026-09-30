package org.group1.coffeeshopapi.payway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Raw PayWay purchase response. status.code is "00" on success and a number on error.
@JsonIgnoreProperties(ignoreUnknown = true)
public record PaywayPurchaseResponse(
        PaywayStatus status,
        @JsonProperty("abapay_deeplink") String abapayDeeplink
) {
}
