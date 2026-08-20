package org.group1.coffeeshopapi.telegram.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.SecurityConstants;
import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.group1.coffeeshopapi.telegram.dto.TelegramUpdate;
import org.group1.coffeeshopapi.telegram.exception.InvalidWebhookSecretException;
import org.group1.coffeeshopapi.telegram.service.TelegramUpdateHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Telegram", description = "Webhook called by Telegram's servers — not intended to be invoked manually")
public class TelegramWebhookController {

    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final TelegramProperties properties;
    private final TelegramUpdateHandler updateHandler;

    @PostMapping(SecurityConstants.TELEGRAM_WEBHOOK_PATH)
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = SECRET_HEADER, required = false) String secretToken,
            @RequestBody TelegramUpdate update) {
        if (properties.getWebhookSecret() == null || properties.getWebhookSecret().isBlank()
                || !properties.getWebhookSecret().equals(secretToken)) {
            throw new InvalidWebhookSecretException("Invalid webhook secret token");
        }
        updateHandler.handle(update);
        return ResponseEntity.ok().build();
    }
}