package org.group1.coffeeshopapi.bakong.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Raw shape of a POST /v1/generate_deeplink_by_qr response from the Bakong Open API.
@JsonIgnoreProperties(ignoreUnknown = true)
public record BakongGenerateDeeplinkResponse(
        int responseCode,
        String responseMessage,
        Integer errorCode,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(String shortLink) {
    }
}
