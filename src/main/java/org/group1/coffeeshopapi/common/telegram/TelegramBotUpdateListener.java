package org.group1.coffeeshopapi.common.telegram;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.config.TelegramBotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TelegramBotUpdateListener implements LongPollingUpdateConsumer {
    private static final Logger log = LoggerFactory.getLogger(TelegramBotUpdateListener.class);

    private final TelegramBotConfig telegramBotConfig;
    private final TelegramClient telegramClient;

    private TelegramBotsLongPollingApplication botsApplication;

    @PostConstruct
    public void start() {
        String token = telegramBotConfig.getToken();
        if (token == null || token.isBlank()) {
            log.info("Telegram bot token not configured; bot listener will not start");
            return;
        }

        try {
            botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(token, this);
            log.info("Telegram bot listener started");
        } catch (TelegramApiException e) {
            log.warn("Failed to start Telegram bot listener: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void stop() {
        if (botsApplication == null) {
            return;
        }
        try {
            botsApplication.close();
        } catch (Exception e) {
            log.warn("Error stopping Telegram bot listener: {}", e.getMessage());
        }
    }

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(this::handleUpdate);
    }

    private void handleUpdate(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText().trim();
        String reply = switch (text.toLowerCase()) {
            case "/start", "/chatid" -> "Your Telegram chat ID is:\n" + chatId
                    + "\n\nGive this to the admin to enable notifications for this chat.";
            default -> "Send /chatid to get this chat's Telegram ID.";
        };

        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(reply)
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Failed to reply to Telegram update: {}", e.getMessage());
        }
    }
}
