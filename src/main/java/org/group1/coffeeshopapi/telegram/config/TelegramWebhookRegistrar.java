package org.group1.coffeeshopapi.telegram.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramWebhookRegistrar implements ApplicationRunner {
    private final TelegramProperties properties;
    private final TelegramApiClient apiClient;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isAutoRegisterWebhook()) {
            return;
        }
        if (properties.getBotToken() == null || properties.getBotToken().isBlank()) {
            log.info("Telegram bot token not configured, skipping webhook registration");
            return;
        }
        apiClient.registerWebhook();
        apiClient.setMyCommands();
    }
}