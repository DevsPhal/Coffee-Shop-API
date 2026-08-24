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
                /menu - View the full menu
                /menu <category> - View items in a category
                /categories - List product categories
                /discounts - See today's discounted items
                /start <code> - Link your account
                /unlink - Unlink your account
                /help - Show this message""";
    }
}