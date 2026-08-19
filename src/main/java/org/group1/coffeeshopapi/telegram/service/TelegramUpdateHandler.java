package org.group1.coffeeshopapi.telegram.service;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.telegram.command.TelegramCommandRegistry;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.group1.coffeeshopapi.telegram.dto.TelegramUpdate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramUpdateHandler {
    private final TelegramCommandRegistry registry;
    private final TelegramApiClient apiClient;

    public void handle(TelegramUpdate update) {
        TelegramMessage message = update.message();
        if (message == null || message.chat() == null || message.text() == null || message.text().isBlank()) {
            return;
        }

        String text = message.text().trim();
        if (!text.startsWith("/")) {
            return;
        }

        String[] parts = text.split("\\s+", 2);
        String commandName = parts[0].split("@")[0];
        String argument = parts.length > 1 ? parts[1] : null;

        String reply = registry.find(commandName)
                .map(command -> command.execute(message, argument))
                .orElse("Unknown command. Send /help to see what I can do.");

        apiClient.sendMessage(message.chat().id(), reply);
    }
}