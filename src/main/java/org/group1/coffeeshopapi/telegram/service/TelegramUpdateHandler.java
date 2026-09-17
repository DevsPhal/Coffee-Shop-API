package org.group1.coffeeshopapi.telegram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.telegram.command.TelegramCommand;
import org.group1.coffeeshopapi.telegram.command.TelegramCommandRegistry;
import org.group1.coffeeshopapi.telegram.dto.TelegramCallbackQuery;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.group1.coffeeshopapi.telegram.dto.TelegramUpdate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramUpdateHandler {
    private static final String UNEXPECTED_ERROR_MESSAGE =
            "⚠️ Sorry, something went wrong on our end. Please try again in a moment.";
    private static final String UNKNOWN_COMMAND_MESSAGE = "❓ Unknown command. Send /help to see what I can do.";

    private final TelegramCommandRegistry registry;
    private final TelegramApiClient apiClient;
    private final TelegramLinkService telegramLinkService;

    // Catch broadly so an unexpected error still gets a reply instead of Telegram retrying the
    // same update forever.
    public void handle(TelegramUpdate update) {
        if (update.callbackQuery() != null) {
            handleCallbackQuery(update.callbackQuery());
            return;
        }

        TelegramMessage message = update.message();
        if (message == null || message.chat() == null) {
            return;
        }

        try {
            dispatch(message);
        } catch (Exception ex) {
            log.error("Failed to handle Telegram message for chat {}", message.chat().id(), ex);
            apiClient.sendMessageWithButtons(message.chat().id(), UNEXPECTED_ERROR_MESSAGE);
        }
    }

    private void dispatch(TelegramMessage message) {
        // A shared contact card, not a command — routed separately.
        if (message.contact() != null) {
            Long senderId = message.from() != null ? message.from().id() : null;
            String reply = telegramLinkService.verifyPendingContact(message.chat().id(), message.contact(), senderId);
            apiClient.sendHtmlMessageWithButtons(message.chat().id(), reply);
            return;
        }

        if (message.text() == null || message.text().isBlank()) {
            return;
        }

        String text = message.text().trim();
        if (!text.startsWith("/")) {
            return;
        }

        String[] parts = text.split("\\s+", 2);
        // Lowercased so "/Menu" still matches "/menu".
        String commandName = parts[0].split("@")[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1] : null;

        Optional<TelegramCommand> command = registry.find(commandName);
        if (command.isEmpty()) {
            apiClient.sendMessageWithButtons(message.chat().id(), UNKNOWN_COMMAND_MESSAGE);
            return;
        }

        // Not Optional::map — a command can legitimately return null (already replied itself),
        // and map would collapse that into "unknown command".
        TelegramCommand matchedCommand = command.get();
        String reply = matchedCommand.execute(message, argument);
        sendReply(message.chat().id(), matchedCommand, reply);
    }

    // A tapped quick-action button, routed through the same command lookup as a typed command.
    private void handleCallbackQuery(TelegramCallbackQuery callbackQuery) {
        // Answered first, unconditionally, so the button's loading spinner always clears.
        apiClient.answerCallbackQuery(callbackQuery.id(), null);

        TelegramMessage source = callbackQuery.message();
        if (source == null || source.chat() == null) {
            return;
        }

        try {
            dispatchCallback(callbackQuery, source);
        } catch (Exception ex) {
            log.error("Failed to handle Telegram callback query for chat {}", source.chat().id(), ex);
            apiClient.sendMessageWithButtons(source.chat().id(), UNEXPECTED_ERROR_MESSAGE);
        }
    }

    private void dispatchCallback(TelegramCallbackQuery callbackQuery, TelegramMessage source) {
        Optional<TelegramCommand> command = registry.find(callbackQuery.data());
        if (command.isEmpty()) {
            apiClient.sendMessageWithButtons(source.chat().id(), UNKNOWN_COMMAND_MESSAGE);
            return;
        }

        // Build a synthetic message carrying the actual tapper's identity, since the callback's
        // own "message" belongs to whoever the keyboard was originally sent to.
        TelegramCommand matchedCommand = command.get();
        TelegramMessage syntheticMessage =
                new TelegramMessage(source.messageId(), source.chat(), callbackQuery.from(), null, null);
        String reply = matchedCommand.execute(syntheticMessage, null);
        sendReply(source.chat().id(), matchedCommand, reply);
    }

    // A null reply means the command already sent its own message — nothing left to do here.
    private void sendReply(Long chatId, TelegramCommand command, String reply) {
        if (reply == null) {
            return;
        }
        if (command.useHtml()) {
            apiClient.sendHtmlMessageWithButtons(chatId, reply);
        } else {
            apiClient.sendMessageWithButtons(chatId, reply);
        }
    }
}
