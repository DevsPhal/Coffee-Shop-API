package org.group1.coffeeshopapi.telegram.command;

import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand implements TelegramCommand {

    @Override
    public String name() {
        return "/help";
    }

    @Override
    public String execute(TelegramMessage message, String argument) {
        return """
                Available commands:
                /start <code> - Link your account
                /unlink - Unlink your account
                /help - Show this message""";
    }
}