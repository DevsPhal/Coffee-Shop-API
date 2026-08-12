package org.group1.coffeeshopapi.notifications.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.config.TelegramBotConfig;
import org.group1.coffeeshopapi.notifications.service.TelegramNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Service
@RequiredArgsConstructor
public class TelegramNotificationServiceImpl implements TelegramNotificationService {
    private static final Logger log = LoggerFactory.getLogger(TelegramNotificationServiceImpl.class);

    private final TelegramClient telegramClient;
    private final TelegramBotConfig telegramBotConfig;

    @Override
    @Async
    public void notifyAdmin(String title, String message) {
        String chatId = telegramBotConfig.getChatId();
        if (chatId == null || chatId.isBlank()) {
            log.debug("Telegram chat id not configured; skipping admin notification");
            return;
        }

        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("*" + title + "*\n" + message)
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Failed to send Telegram admin notification: {}", e.getMessage());
        }
    }
}
