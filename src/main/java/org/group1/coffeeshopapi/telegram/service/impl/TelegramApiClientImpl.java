package org.group1.coffeeshopapi.telegram.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TelegramApiClientImpl implements TelegramApiClient {

    private final TelegramProperties properties;
    private final RestClient restClient = RestClient.create();

    public TelegramApiClientImpl(TelegramProperties properties) {
        this.properties = properties;
    }

    @Override
    public void sendMessage(Long chatId, String text) {
        call("sendMessage", chatId, Map.of("chat_id", chatId, "text", text));
    }

    @Override
    public void sendHtmlMessage(Long chatId, String html) {
        call("sendMessage", chatId, Map.of("chat_id", chatId, "text", html, "parse_mode", "HTML"));
    }

    @Override
    public void sendPhoto(Long chatId, String photoUrl, String captionHtml) {
        Map<String, Object> body = captionHtml == null || captionHtml.isBlank()
                ? Map.of("chat_id", chatId, "photo", photoUrl)
                : Map.of("chat_id", chatId, "photo", photoUrl, "caption", captionHtml, "parse_mode", "HTML");
        call("sendPhoto", chatId, body);
    }

    @Override
    public void sendLocation(Long chatId, BigDecimal latitude, BigDecimal longitude) {
        call("sendLocation", chatId, Map.of("chat_id", chatId, "latitude", latitude, "longitude", longitude));
    }

    @Override
    public void sendContactRequest(Long chatId, String text) {
        Map<String, Object> keyboard = Map.of(
                "keyboard", List.of(List.of(Map.of("text", "📱 Share phone number", "request_contact", true))),
                "one_time_keyboard", true,
                "resize_keyboard", true);
        call("sendMessage", chatId, Map.of("chat_id", chatId, "text", text, "reply_markup", keyboard));
    }

    private void call(String method, Long chatId, Map<String, ?> body) {
        if (!hasToken()) {
            return;
        }
        try {
            restClient.post()
                    .uri(properties.apiUrl(method))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.error("Failed to call Telegram {} for chat {}", method, chatId, ex);
        }
    }

    @Override
    public void registerWebhook() {
        if (!hasToken() || properties.getWebhookBaseUrl() == null || properties.getWebhookBaseUrl().isBlank()) {
            log.warn("Telegram webhook base URL not configured, skipping setWebhook");
            return;
        }
        try {
            restClient.post()
                    .uri(properties.apiUrl("setWebhook"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "url", properties.fullWebhookUrl(),
                            "secret_token", properties.getWebhookSecret(),
                            "allowed_updates", List.of("message")))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Telegram webhook registered at {}", properties.fullWebhookUrl());
        } catch (Exception ex) {
            log.error("Failed to register Telegram webhook", ex);
        }
    }

    @Override
    public void setMyCommands() {
        if (!hasToken()) {
            return;
        }
        try {
            List<Map<String, String>> commands = List.of(
                    Map.of("command", "menu", "description", "View the menu (optionally by category)"),
                    Map.of("command", "categories", "description", "List product categories"),
                    Map.of("command", "discounts", "description", "See today's discounted items"),
                    Map.of("command", "events", "description", "See upcoming events"),
                    Map.of("command", "rate", "description", "Current USD to KHR exchange rate"),
                    Map.of("command", "start", "description", "Link your account"),
                    Map.of("command", "unlink", "description", "Unlink your account"),
                    Map.of("command", "help", "description", "Show available commands")
            );
            restClient.post()
                    .uri(properties.apiUrl("setMyCommands"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("commands", commands))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.error("Failed to register Telegram bot commands", ex);
        }
    }

    private boolean hasToken() {
        return properties.getBotToken() != null && !properties.getBotToken().isBlank();
    }
}