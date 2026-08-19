package org.group1.coffeeshopapi.telegram.service;

public interface TelegramApiClient {
    void sendMessage(Long chatId, String text);
    void registerWebhook();
    void setMyCommands();
}