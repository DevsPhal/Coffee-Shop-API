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

    // Telegram treats anything but a 2xx response from the webhook as delivery failure and keeps
    // retrying the same update — so letting an unexpected exception (a bug in a command, a
    // transient DB hiccup, ...) propagate out of here would make TelegramWebhookController return
    // a 500, and Telegram would hammer the same update at us again while the chat never gets any
    // reply at all. Catching broadly here instead means the webhook always finishes cleanly and
    // the user always gets some reply. Commands with an expected failure mode (e.g. StartCommand,
    // UnlinkCommand) already catch ApiException themselves and return its message as a normal
    // reply, so this only ever catches the genuinely unexpected ones.
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
        // A shared contact card (tapped from the request_contact keyboard TelegramLinkService
        // prompted with) — not a command, so it's routed separately before the text/command check.
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
        // Lowercased: every registered command name is lowercase (see TelegramCommand
        // implementations / BotFather convention), but nothing stops a user from typing "/Menu" —
        // that should still work, not silently fall through to "Unknown command".
        String commandName = parts[0].split("@")[0].toLowerCase();
        String argument = parts.length > 1 ? parts[1] : null;

        Optional<TelegramCommand> command = registry.find(commandName);
        if (command.isEmpty()) {
            apiClient.sendMessageWithButtons(message.chat().id(), UNKNOWN_COMMAND_MESSAGE);
            return;
        }

        // Not Optional::map: a matched command legitimately returning null (it already sent its
        // own message directly — e.g. the contact-request keyboard for a pending staff invite,
        // see TelegramLinkServiceImpl#resolveLinkCode) must stay distinguishable from "no command
        // matched" — Optional.map collapses a null-returning mapper into empty, which would make
        // that case fall through to the "Unknown command" reply below it instead of sending
        // nothing.
        TelegramCommand matchedCommand = command.get();
        String reply = matchedCommand.execute(message, argument);
        sendReply(message.chat().id(), matchedCommand, reply);
    }

    // A tapped quick-action button (see TelegramApiClientImpl's QUICK_ACTIONS_KEYBOARD) — routed
    // through the exact same TelegramCommandRegistry lookup as a typed command, keyed by the
    // button's callback_data (its command name).
    private void handleCallbackQuery(TelegramCallbackQuery callbackQuery) {
        // Answered unconditionally, before anything that could fail: Telegram leaves the tapped
        // button showing a loading spinner until this is called, regardless of how the tap
        // ultimately turns out.
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

        // A button carries no free-text argument — same contract as typing the command with none.
        // The callback query's own "message" is the one the keyboard was attached to, not one
        // authored by whoever tapped it, so build a synthetic message carrying the tapper's own
        // identity (commands that care who's asking read message.from(), not the callback query).
        TelegramCommand matchedCommand = command.get();
        TelegramMessage syntheticMessage =
                new TelegramMessage(source.messageId(), source.chat(), callbackQuery.from(), null, null);
        String reply = matchedCommand.execute(syntheticMessage, null);
        sendReply(source.chat().id(), matchedCommand, reply);
    }

    // A null reply means the command already sent its own message directly (e.g. the
    // contact-request keyboard for a pending staff invite — see
    // TelegramLinkServiceImpl#resolveLinkCode) — nothing left to send here.
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
