package org.group1.coffeeshopapi.telegram.service.impl;

import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;

// No bot token is configured in tests, so this just guards that every send method takes the
// same early-return path instead of NPEing on a null token.
class TelegramApiClientImplTest {

    private final TelegramApiClientImpl client = new TelegramApiClientImpl(new TelegramProperties());

    @Test
    void sendPhotoIsANoOpWhenNoBotTokenIsConfigured() {
        assertThatCode(() -> client.sendPhoto(123L, "https://example.test/flyer.png", "<b>Event</b>"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendPhotoIsANoOpWithoutACaptionToo() {
        assertThatCode(() -> client.sendPhoto(123L, "https://example.test/flyer.png", null))
                .doesNotThrowAnyException();
    }

    @Test
    void sendLocationIsANoOpWhenNoBotTokenIsConfigured() {
        assertThatCode(() -> client.sendLocation(123L, new BigDecimal("11.5568"), new BigDecimal("104.9282")))
                .doesNotThrowAnyException();
    }

    @Test
    void sendMessageWithButtonsIsANoOpWhenNoBotTokenIsConfigured() {
        assertThatCode(() -> client.sendMessageWithButtons(123L, "pick one"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendHtmlMessageWithButtonsIsANoOpWhenNoBotTokenIsConfigured() {
        assertThatCode(() -> client.sendHtmlMessageWithButtons(123L, "<b>pick one</b>"))
                .doesNotThrowAnyException();
    }

    @Test
    void answerCallbackQueryIsANoOpWhenNoBotTokenIsConfigured() {
        assertThatCode(() -> client.answerCallbackQuery("cbq-1", null))
                .doesNotThrowAnyException();
        assertThatCode(() -> client.answerCallbackQuery("cbq-1", "Done!"))
                .doesNotThrowAnyException();
    }
}
