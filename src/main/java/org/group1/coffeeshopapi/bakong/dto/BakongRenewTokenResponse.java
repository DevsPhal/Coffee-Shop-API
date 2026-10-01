package org.group1.coffeeshopapi.bakong.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
