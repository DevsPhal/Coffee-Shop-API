package org.group1.coffeeshopapi.telegram.service.impl;

import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * No bot token is configured in any test profile, so every call here takes the same early-return
 * path as the existing sendMessage/sendContactRequest methods (see TelegramApiClientImpl#call) —
 * this only guards that sendPhoto/sendLocation follow that same no-token-configured contract
 * instead of NPEing on a null bot token.
 */
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
}
