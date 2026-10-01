package org.group1.coffeeshopapi.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TelegramWidgetAuthRequest(
        @NotNull(message = "Telegram id is required")
        Long id,

        @NotBlank(message = "First name is required")
        @JsonProperty("first_name")
        String firstName,

        @JsonProperty("last_name")
        String lastName,

        String username,

        @JsonProperty("photo_url")
        String photoUrl,

        @NotNull(message = "auth_date is required")
        @JsonProperty("auth_date")
        Long authDate,

        @NotBlank(message = "hash is required")
        String hash
) {
}
