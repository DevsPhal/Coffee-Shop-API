package org.group1.coffeeshopapi.telegram.command;

import org.group1.coffeeshopapi.telegram.dto.incoming.TelegramMessage;

public interface TelegramCommand {

    /** e.g. "start" for /start — no slash, lowercase */
    String name();

    void execute(TelegramMessage message, String argument);
}
