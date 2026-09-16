package org.group1.coffeeshopapi.telegram.service;

import java.math.BigDecimal;

public interface TelegramApiClient {
    void sendMessage(Long chatId, String text);

    // Same delivery, but with Telegram's HTML parse mode on — used for menu/discount/invoice
    // messages that need bold headers, bullets, and strikethrough prices. Callers are responsible
    // for HTML-escaping any interpolated user/admin-entered text (product names, etc.).
    void sendHtmlMessage(Long chatId, String html);

    // Same as sendMessage/sendHtmlMessage, but with the bot's persistent quick-action inline
    // keyboard (see TelegramApiClientImpl#QUICK_ACTIONS_KEYBOARD) attached below the text — one
    // button per command, so a chat can keep browsing by tapping instead of typing. Used for every
    // command reply (see TelegramUpdateHandler) so the keyboard is always one tap away.
    void sendMessageWithButtons(Long chatId, String text);
    void sendHtmlMessageWithButtons(Long chatId, String html);

    // Sends an image (by its publicly reachable URL — see FileStorageService) with an optional
    // HTML-parse-mode caption underneath, e.g. an event's flyer. Caller is responsible for
    // HTML-escaping any interpolated text, same as sendHtmlMessage.
    void sendPhoto(Long chatId, String photoUrl, String captionHtml);

    // Drops a map pin at the given coordinates — e.g. an event's venue, sent right after its
    // announcement/reminder message.
    void sendLocation(Long chatId, BigDecimal latitude, BigDecimal longitude);

    // Prompts the chat with a native "Share phone number" button (Telegram's request_contact
    // keyboard) — used to verify a Telegram-invited staff member's phone number matches the one
    // their admin entered at invite time (see TelegramLinkServiceImpl#verifyPendingContact).
    void sendContactRequest(Long chatId, String text);

    // Clears the loading spinner Telegram puts on a tapped inline button until this is called —
    // required after every callback query, win or lose. toastText is an optional small popup
    // shown over the chat (not a chat message); pass null/blank for a silent acknowledgement.
    void answerCallbackQuery(String callbackQueryId, String toastText);

    void registerWebhook();
    void setMyCommands();
}