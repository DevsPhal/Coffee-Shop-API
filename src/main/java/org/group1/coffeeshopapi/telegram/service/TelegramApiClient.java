package org.group1.coffeeshopapi.telegram.service;

public interface TelegramApiClient {
    void sendMessage(Long chatId, String text);

    // Same delivery, but with Telegram's HTML parse mode on — used for menu/discount/invoice
    // messages that need bold headers, bullets, and strikethrough prices. Callers are responsible
    // for HTML-escaping any interpolated user/admin-entered text (product names, etc.).
    void sendHtmlMessage(Long chatId, String html);

    void registerWebhook();
    void setMyCommands();
}