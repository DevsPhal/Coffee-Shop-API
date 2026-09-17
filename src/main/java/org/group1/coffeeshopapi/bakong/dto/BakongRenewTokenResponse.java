package org.group1.coffeeshopapi.bakong.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Raw shape of a /v1/renew_token response. responseCode 0 means success, with the new token
// in data.token.
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
