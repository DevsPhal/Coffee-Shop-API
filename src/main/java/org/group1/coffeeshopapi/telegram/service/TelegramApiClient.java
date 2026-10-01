package org.group1.coffeeshopapi.telegram.service;

import java.math.BigDecimal;

public interface TelegramApiClient {
    void sendMessage(Long chatId, String text);

    void sendHtmlMessage(Long chatId, String html);

    void sendMessageWithButtons(Long chatId, String text);
    void sendHtmlMessageWithButtons(Long chatId, String html);

    void sendPhoto(Long chatId, String photoUrl, String captionHtml);

    void sendLocation(Long chatId, BigDecimal latitude, BigDecimal longitude);

    void sendContactRequest(Long chatId, String text);

    void answerCallbackQuery(String callbackQueryId, String toastText);

    void registerWebhook();
    void setMyCommands();
}
