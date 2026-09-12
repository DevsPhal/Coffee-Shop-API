package org.group1.coffeeshopapi.bakong.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Raw shape of a POST /v1/renew_token response from the Bakong Open API. A success is
 * {@code responseCode == 0} with the new token in {@code data.token}; failures come back with
 * {@code responseCode == 1} and a reason such as "Not registered yet".
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BakongRenewTokenResponse(
        int responseCode,
        String responseMessage,
        Integer errorCode,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(String token) {
    }
}
