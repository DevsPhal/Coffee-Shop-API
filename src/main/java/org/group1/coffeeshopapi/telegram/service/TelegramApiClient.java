package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.telegram.dto.TelegramProfilePhoto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TelegramApiClient {
    void sendMessage(Long chatId, String text);

    void sendHtmlMessage(Long chatId, String html);

    void sendHtmlMessageOrThrow(Long chatId, String html);

    void sendMessageWithButtons(Long chatId, String text);
    void sendHtmlMessageWithButtons(Long chatId, String html);

    void sendHtmlMessageWithInlineKeyboard(Long chatId, String html, List<List<Map<String, String>>> keyboard);

    void editMessageText(Long chatId, Long messageId, String html);

    void sendPhoto(Long chatId, String photoUrl, String captionHtml);

    void sendLocation(Long chatId, BigDecimal latitude, BigDecimal longitude);

    void sendContactRequest(Long chatId, String text);

    void answerCallbackQuery(String callbackQueryId, String toastText);

    /**
     * The user's current profile photo, or empty when they have none the bot is allowed to see.
     * Throws {@link org.group1.coffeeshopapi.telegram.exception.TelegramDeliveryException} when Telegram can't be asked.
     */
    Optional<TelegramProfilePhoto> findProfilePhoto(Long userId);

    /** Downloads a file the Bot API handed out. The download URL embeds the bot token, so it never leaves this class. */
    byte[] downloadFile(String fileId, long maxBytes);

    /** Downloads the Login Widget's photo_url, refusing anything not served over HTTPS by Telegram itself. */
    byte[] downloadWidgetPhoto(String photoUrl, long maxBytes);

    void registerWebhook();
    void setMyCommands();
}
