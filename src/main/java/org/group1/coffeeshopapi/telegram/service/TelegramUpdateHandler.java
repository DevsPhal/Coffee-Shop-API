package org.group1.coffeeshopapi.telegram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.telegram.command.TelegramCommand;
import org.group1.coffeeshopapi.telegram.command.TelegramCommandRegistry;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.group1.coffeeshopapi.telegram.dto.TelegramUpdate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramUpdateHandler {
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
        TelegramMessage message = update.message();
        if (message == null || message.chat() == null) {
            return;
        }

        try {
            dispatch(message);
        } catch (Exception ex) {
            log.error("Failed to handle Telegram update for chat {}", message.chat().id(), ex);
            apiClient.sendMessage(message.chat().id(),
                    "Sorry, something went wrong on our end. Please try again in a moment.");
        }
    }

    private void dispatch(TelegramMessage message) {
        // A shared contact card (tapped from the request_contact keyboard TelegramLinkService
        // prompted with) — not a command, so it's routed separately before the text/command check.
        if (message.contact() != null) {
            Long senderId = message.from() != null ? message.from().id() : null;
            String reply = telegramLinkService.verifyPendingContact(message.chat().id(), message.contact(), senderId);
            apiClient.sendHtmlMessage(message.chat().id(), reply);
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
            apiClient.sendMessage(message.chat().id(), "Unknown command. Send /help to see what I can do.");
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
        if (reply == null) {
            return;
        }

        if (matchedCommand.useHtml()) {
            apiClient.sendHtmlMessage(message.chat().id(), reply);
        } else {
            apiClient.sendMessage(message.chat().id(), reply);
        }
    }
}