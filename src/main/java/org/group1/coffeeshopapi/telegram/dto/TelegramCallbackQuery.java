package org.group1.coffeeshopapi.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

// Sent when a user taps an inline keyboard button. "from" is who tapped it; "data" is the
// button's callback value, treated like a typed command.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TelegramCallbackQuery(String id, TelegramUser from, TelegramMessage message, String data) {
}
