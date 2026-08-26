package org.group1.coffeeshopapi.telegram.command;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.exception.ApiException;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.group1.coffeeshopapi.telegram.util.TelegramFormat;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommand implements TelegramCommand {

    private static final String WELCOME_NOT_LINKED = """
            👋 <b>Welcome to 590st Cafe!</b>

            Browse right away with /menu, /categories, /discounts, /events or /rate — no account needed.

            To link your account and get order receipts + event alerts here, generate a code in the app (Profile → Link Telegram), then send it back as /start &lt;code&gt;.

            Send /help any time to see everything I can do.""";

    private final TelegramLinkService telegramLinkService;

    @Override
    public String name() {
        return "/start";
    }

    @Override
    public String execute(TelegramMessage message, String argument) {
        Long chatId = message.chat().id();

        // No code given — behavior depends entirely on whether this chat is already linked.
        if (argument == null || argument.isBlank()) {
            return telegramLinkService.linkedCustomerName(chatId)
                    .map(this::welcomeBack)
                    .orElse(WELCOME_NOT_LINKED);
        }

        // A code was given — resolveLinkCode itself handles all three outcomes: not linked yet
        // (links now), already linked to this same account (no-op, says so), or linked to a
        // different account (switches, unlinking the old one first).
        try {
            return telegramLinkService.resolveLinkCode(argument.trim(), chatId);
        } catch (ApiException ex) {
            return ex.getMessage();
        }
    }

    private String welcomeBack(String customerName) {
        return "👋 <b>Welcome back, " + TelegramFormat.escape(TelegramFormat.titleCase(customerName)) + "!</b>\n\n"
                + "Your account is already linked here. Try /menu, /events or /rate — or send /help to see everything I can do.\n\n"
                + "Want to link a different account instead? Send /unlink first, then /start &lt;code&gt; with a fresh code.";
    }

    @Override
    public boolean useHtml() {
        return true;
    }
}
