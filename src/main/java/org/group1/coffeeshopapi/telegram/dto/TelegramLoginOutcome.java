package org.group1.coffeeshopapi.telegram.dto;

import org.group1.coffeeshopapi.common.enums.TelegramLoginStatus;

public record TelegramLoginOutcome(TelegramLoginStatus status, TelegramUser telegramUser) {

    public static TelegramLoginOutcome of(TelegramLoginStatus status) {
        return new TelegramLoginOutcome(status, null);
    }

    public static TelegramLoginOutcome approved(TelegramUser telegramUser) {
        return new TelegramLoginOutcome(TelegramLoginStatus.APPROVED, telegramUser);
    }
}
