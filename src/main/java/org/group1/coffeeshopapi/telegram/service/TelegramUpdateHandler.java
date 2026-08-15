package org.group1.coffeeshopapi.telegram.service;


import org.group1.coffeeshopapi.telegram.command.TelegramCommand;
import org.group1.coffeeshopapi.telegram.command.TelegramCommandRegistry;
import org.group1.coffeeshopapi.telegram.dto.incoming.TelegramMessage;
import org.group1.coffeeshopapi.telegram.dto.incoming.TelegramUpdate;
import org.springframework.stereotype.Service;

@Service
public class TelegramUpdateHandler {

    private final TelegramCommandRegistry commandRegistry;
    private final TelegramApiClient apiClient;

    public TelegramUpdateHandler(TelegramCommandRegistry commandRegistry, TelegramApiClient apiClient) {
        this.commandRegistry = commandRegistry;
        this.apiClient = apiClient;
    }

    public void handle(TelegramUpdate update) {
        TelegramMessage message = update.getMessage();
        if (message == null || message.getText() == null || message.getChat() == null) {
            return;
        }

        String text = message.getText().trim();
        if (!text.startsWith("/")) {
            apiClient.sendMessage(message.getChat().getId(), "Send /help to see available commands.");
            return;
        }

        String[] parts = text.substring(1).split("\\s+", 2);
        String commandToken = parts[0].split("@")[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1].trim() : null;

        TelegramCommand command = commandRegistry.find(commandToken);
        if (command == null) {
            apiClient.sendMessage(message.getChat().getId(),
                    "Unknown command <code>/" + commandToken + "</code>. Send /help to see what I can do.");
            return;
        }

        command.execute(message, argument);
    }
}
