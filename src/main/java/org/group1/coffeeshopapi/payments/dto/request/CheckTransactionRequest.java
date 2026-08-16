package org.group1.coffeeshopapi.payments.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CheckTransactionRequest(
        @NotBlank(message = "MD5 is required")
        String md5) {
}
