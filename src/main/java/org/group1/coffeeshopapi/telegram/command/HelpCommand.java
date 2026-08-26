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
                🤖 <b>Available Commands</b>

                <b>Browse</b>
                /menu — View the full menu
                /menu &lt;category&gt; — View items in a category
                /categories — List product categories
                /discounts — See today's discounted items
                /events — See upcoming events
                /rate — Current USD → KHR exchange rate

                <b>Account</b>
                /start &lt;code&gt; — Link your account
                /unlink — Unlink your account

                /help — Show this message""";
    }

    @Override
    public boolean useHtml() {
        return true;
    }
}
