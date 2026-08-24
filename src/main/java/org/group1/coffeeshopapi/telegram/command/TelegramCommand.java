package org.group1.coffeeshopapi.telegram.command;

import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;

public interface TelegramCommand {
    String name();
    String execute(TelegramMessage message, String argument);

    // True for replies that use Telegram HTML formatting (bold headers, bullets, strikethrough
    // prices) rather than plain text.
    default boolean useHtml() {
        return false;
    }
}