package org.group1.coffeeshopapi.telegram.service;

import java.math.BigDecimal;

public interface TelegramApiClient {
    void sendMessage(Long chatId, String text);

    // Same as sendMessage, but with Telegram's HTML formatting turned on.
    void sendHtmlMessage(Long chatId, String html);

    // Same as sendMessage/sendHtmlMessage, but with the quick-action inline keyboard attached.
    void sendMessageWithButtons(Long chatId, String text);
    void sendHtmlMessageWithButtons(Long chatId, String html);

    // Sends an image by URL, with an optional HTML caption underneath.
    void sendPhoto(Long chatId, String photoUrl, String captionHtml);

    // Drops a map pin at the given coordinates.
    void sendLocation(Long chatId, BigDecimal latitude, BigDecimal longitude);

    // Prompts the chat with a native "Share phone number" button.
    void sendContactRequest(Long chatId, String text);

    // Clears the loading spinner on a tapped inline button. toastText is an optional small popup;
    // pass null for a silent acknowledgement.
    void answerCallbackQuery(String callbackQueryId, String toastText);

    void registerWebhook();
    void setMyCommands();
}