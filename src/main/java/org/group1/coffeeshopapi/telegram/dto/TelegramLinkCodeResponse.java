package org.group1.coffeeshopapi.telegram.dto;

public record TelegramLinkCodeResponse(String code, int expiresInSeconds, String deepLink) {
}