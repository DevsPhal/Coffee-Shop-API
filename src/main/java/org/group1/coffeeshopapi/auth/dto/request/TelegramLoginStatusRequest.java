package org.group1.coffeeshopapi.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TelegramLoginStatusRequest(
        @NotBlank(message = "Request id is required")
        String requestId,

        @NotBlank(message = "Poll token is required")
        String pollToken
) {
}
