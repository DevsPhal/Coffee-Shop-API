package org.group1.coffeeshopapi.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * The signed payload Telegram's Login Widget hands back once the user authorizes — see
 * https://core.telegram.org/widgets/login. Field names/casing match Telegram's own JSON exactly,
 * so the frontend can forward the widget callback's object straight through unchanged.
 * lastName/username/photoUrl are genuinely optional — Telegram omits them from both the payload
 * and its own signature when absent, so leave them out of the request body entirely rather than
 * sending an empty string (see TelegramWidgetAuthVerifier).
 */
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
