package org.group1.coffeeshopapi.telegram.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.group1.coffeeshopapi.telegram.exception.TelegramDeliveryException;
import org.group1.coffeeshopapi.telegram.exception.TelegramDeliveryException.Reason;
import org.group1.coffeeshopapi.telegram.service.TelegramApiClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TelegramApiClientImpl implements TelegramApiClient {

    private static final Map<String, Object> QUICK_ACTIONS_KEYBOARD = Map.of("inline_keyboard", List.of(
            List.of(button("🛒 Menu", "/menu"), button("🗂 Categories", "/categories")),
            List.of(button("🔥 Discounts", "/discounts"), button("🎉 Events", "/events")),
            List.of(button("💱 Rate", "/rate"), button("❓ Help", "/help")),
            List.of(button("🔗 Link Account", "/start"), button("🔓 Unlink", "/unlink"))
    ));

    private static Map<String, String> button(String text, String callbackData) {
        return Map.of("text", text, "callback_data", callbackData);
    }

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
    public void sendHtmlMessageOrThrow(Long chatId, String html) {
        if (!properties.hasBotToken()) {
            log.error("Telegram bot token is not configured — cannot deliver a message to chat {}", chatId);
            throw notConfigured();
        }
        try {
            post("sendMessage", Map.of("chat_id", chatId, "text", html, "parse_mode", "HTML"));
        } catch (HttpStatusCodeException ex) {
            log.warn("Telegram sendMessage to chat {} failed: {}", chatId, describe(ex));
            throw classify(ex);
        } catch (RestClientException ex) {
            log.warn("Telegram sendMessage to chat {} failed: {}", chatId, describe(ex));
            throw unavailable();
        }
    }

    @Override
    public void sendMessageWithButtons(Long chatId, String text) {
        call("sendMessage", chatId, Map.of("chat_id", chatId, "text", text, "reply_markup", QUICK_ACTIONS_KEYBOARD));
    }

    @Override
    public void sendHtmlMessageWithButtons(Long chatId, String html) {
        call("sendMessage", chatId, Map.of("chat_id", chatId, "text", html, "parse_mode", "HTML",
                "reply_markup", QUICK_ACTIONS_KEYBOARD));
    }

    @Override
    public void sendHtmlMessageWithInlineKeyboard(Long chatId, String html, List<List<Map<String, String>>> keyboard) {
        call("sendMessage", chatId, Map.of("chat_id", chatId, "text", html, "parse_mode", "HTML",
                "reply_markup", Map.of("inline_keyboard", keyboard)));
    }

    @Override
    public void editMessageText(Long chatId, Long messageId, String html) {
        call("editMessageText", chatId, Map.of("chat_id", chatId, "message_id", messageId, "text", html,
                "parse_mode", "HTML"));
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

    @Override
    public void answerCallbackQuery(String callbackQueryId, String toastText) {
        Map<String, Object> body = toastText == null || toastText.isBlank()
                ? Map.of("callback_query_id", callbackQueryId)
                : Map.of("callback_query_id", callbackQueryId, "text", toastText);
        call("answerCallbackQuery", null, body);
    }

    private void call(String method, Long chatId, Map<String, ?> body) {
        if (!properties.hasBotToken()) {
            return;
        }
        try {
            post(method, body);
        } catch (Exception ex) {
            log.error("Failed to call Telegram {} for chat {}: {}", method, chatId, describe(ex));
        }
    }

    private void post(String method, Map<String, ?> body) {
        restClient.post()
                .uri(properties.apiUrl(method))
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void registerWebhook() {
        if (!properties.hasBotToken() || properties.getWebhookBaseUrl() == null || properties.getWebhookBaseUrl().isBlank()) {
            log.warn("Telegram webhook base URL not configured, skipping setWebhook");
            return;
        }
        if (!properties.hasWebhookSecret()) {
            log.error("TELEGRAM_WEBHOOK_SECRET is not set, skipping setWebhook — without it the webhook rejects "
                    + "every update, so bot commands and Telegram login cannot work");
            return;
        }
        try {
            post("setWebhook", Map.of(
                    "url", properties.fullWebhookUrl(),
                    "secret_token", properties.getWebhookSecret(),
                    "allowed_updates", List.of("message", "callback_query")));
            log.info("Telegram webhook registered at {}", properties.fullWebhookUrl());
        } catch (Exception ex) {
            log.error("Failed to register Telegram webhook: {}", describe(ex));
        }
    }

    @Override
    public void setMyCommands() {
        if (!properties.hasBotToken()) {
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
            post("setMyCommands", Map.of("commands", commands));
        } catch (Exception ex) {
            log.error("Failed to register Telegram bot commands: {}", describe(ex));
        }
    }

    private TelegramDeliveryException classify(HttpStatusCodeException ex) {
        int status = ex.getStatusCode().value();
        if (status == 401 || status == 404) {
            log.error("Telegram rejected the bot token — check TELEGRAM_BOT_TOKEN");
            return notConfigured();
        }
        if (status == 403 || (status == 400 && ex.getResponseBodyAsString().toLowerCase().contains("chat not found"))) {
            return new TelegramDeliveryException(Reason.CHAT_UNREACHABLE,
                    "We couldn't reach your Telegram chat. Open " + botHandle()
                            + " in Telegram, tap Start (or Unblock if you blocked it), then try again.");
        }
        return unavailable();
    }

    private TelegramDeliveryException notConfigured() {
        return new TelegramDeliveryException(Reason.NOT_CONFIGURED,
                "Telegram sign-in isn't available right now. Please use another sign-in method or try again later.");
    }

    private TelegramDeliveryException unavailable() {
        return new TelegramDeliveryException(Reason.UNAVAILABLE,
                "Telegram couldn't be reached right now. Please try again in a moment.");
    }

    private String botHandle() {
        String username = properties.plainBotUsername();
        return username == null || username.isBlank() ? "our bot" : "@" + username;
    }

    private String describe(Exception ex) {
        String detail = ex instanceof HttpStatusCodeException http
                ? http.getStatusCode().value() + " " + http.getResponseBodyAsString()
                : ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return properties.hasBotToken() ? detail.replace(properties.getBotToken(), "<redacted>") : detail;
    }
}
