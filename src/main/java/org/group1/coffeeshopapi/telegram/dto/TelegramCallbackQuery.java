package org.group1.coffeeshopapi.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

// Sent when a user taps an inline keyboard button (see TelegramApiClientImpl's
// QUICK_ACTIONS_KEYBOARD). "message" is the original message the keyboard was attached to — not
// authored by whoever tapped it, so it carries the right chat but the wrong sender; "from" is the
// actual tapper. "data" is that button's callback_data, which TelegramUpdateHandler looks up in
// TelegramCommandRegistry exactly like a typed command name.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TelegramCallbackQuery(String id, TelegramUser from, TelegramMessage message, String data) {
}
