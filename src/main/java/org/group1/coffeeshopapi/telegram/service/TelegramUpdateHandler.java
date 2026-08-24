package org.group1.coffeeshopapi.telegram.service;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.telegram.command.TelegramCommand;
import org.group1.coffeeshopapi.telegram.command.TelegramCommandRegistry;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.group1.coffeeshopapi.telegram.dto.TelegramUpdate;
import org.springframework.stereotype.Component;

import java.util.Optional;

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

        Optional<TelegramCommand> command = registry.find(commandName);
        String reply = command
                .map(c -> c.execute(message, argument))
                .orElse("Unknown command. Send /help to see what I can do.");

        if (command.map(TelegramCommand::useHtml).orElse(false)) {
            apiClient.sendHtmlMessage(message.chat().id(), reply);
        } else {
            apiClient.sendMessage(message.chat().id(), reply);
        }
    }
}