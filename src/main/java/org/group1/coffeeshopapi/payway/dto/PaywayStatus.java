package org.group1.coffeeshopapi.payway.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaywayStatus(Object code, String message) {

    public boolean isSuccess() {
        return code != null && "00".equals(String.valueOf(code));
    }
}
