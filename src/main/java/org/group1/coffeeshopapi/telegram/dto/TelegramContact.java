package org.group1.coffeeshopapi.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

// Sent by Telegram when a user taps a "request_contact" keyboard button (see
// TelegramApiClient#sendContactRequest) to share their own phone number.
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TelegramContact(String phoneNumber, Long userId, String firstName, String lastName) {
}
