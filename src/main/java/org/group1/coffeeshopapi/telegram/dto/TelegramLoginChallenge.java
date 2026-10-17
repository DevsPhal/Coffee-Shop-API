package org.group1.coffeeshopapi.telegram.dto;

public record TelegramLoginChallenge(
        String requestId,
        String pollToken,
        String deepLink,
        String botUsername,
        int expiresInSeconds
) {
}
