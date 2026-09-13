package org.group1.coffeeshopapi.telegram.service;

public interface TelegramApiClient {
    void sendMessage(Long chatId, String text);

    // Same delivery, but with Telegram's HTML parse mode on — used for menu/discount/invoice
    // messages that need bold headers, bullets, and strikethrough prices. Callers are responsible
    // for HTML-escaping any interpolated user/admin-entered text (product names, etc.).
    void sendHtmlMessage(Long chatId, String html);

    // Prompts the chat with a native "Share phone number" button (Telegram's request_contact
    // keyboard) — used to verify a Telegram-invited staff member's phone number matches the one
    // their admin entered at invite time (see TelegramLinkServiceImpl#verifyPendingContact).
    void sendContactRequest(Long chatId, String text);

    void registerWebhook();
    void setMyCommands();
}