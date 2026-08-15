package org.group1.coffeeshopapi.telegram.command;

import org.group1.coffeeshopapi.telegram.dto.incoming.TelegramMessage;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.group1.coffeeshopapi.telegram.service.TelegramLinkService;
import org.springframework.stereotype.Component;

@Component
public class StartCommand implements TelegramCommand {

    private final TelegramLinkService linkService;
    private final TelegramApiClient apiClient;

    public StartCommand(TelegramLinkService linkService, TelegramApiClient apiClient) {
        this.linkService = linkService;
        this.apiClient = apiClient;
    }

    @Override
    public String name() { return "start"; }

    @Override
    public void execute(TelegramMessage message, String argument) {
        Long chatId = message.getChat().getId();

        if (argument == null || argument.isBlank()) {
            apiClient.sendMessage(chatId,
                    "👋 Welcome to <b>590st Cafe</b>!\n\n" +
                            "To link your account, go to the website, tap \"Link Telegram\", " +
                            "and send the code you get here as:\n<code>/start YOURCODE</code>\n\n" +
                            "Send /help to see what I can do.");
            return;
        }

        String username = message.getFrom() != null ? message.getFrom().getUsername() : null;
        TelegramLinkService.LinkResult result = linkService.resolveCode(argument.trim(), chatId, username);

        if (result.ok()) {
            apiClient.sendMessage(chatId,
                    "✅ Your Telegram is now linked to your 590st Cafe account.\n" +
                            "You'll receive your order receipts here. Send /help for more.");
        } else {
            apiClient.sendMessage(chatId,
                    "❌ That code is invalid or expired. Please generate a new one on the website.");
        }
    }
}
