package org.group1.coffeeshopapi.auth.dto.response;

import org.group1.coffeeshopapi.common.enums.TelegramLoginStatus;

public record TelegramLoginStatusResponse(TelegramLoginStatus status, AuthTokenResponse tokens) {

    public static TelegramLoginStatusResponse of(TelegramLoginStatus status) {
        return new TelegramLoginStatusResponse(status, null);
    }

    public static TelegramLoginStatusResponse approved(AuthTokenResponse tokens) {
        return new TelegramLoginStatusResponse(TelegramLoginStatus.APPROVED, tokens);
    }
}
