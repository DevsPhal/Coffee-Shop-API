package org.group1.coffeeshopapi.telegram.controller;


import jakarta.servlet.http.HttpServletRequest;
import org.group1.coffeeshopapi.telegram.config.TelegramProperties;
import org.group1.coffeeshopapi.telegram.dto.incoming.TelegramUpdate;
import org.group1.coffeeshopapi.telegram.exception.InvalidWebhookSecretException;
import org.group1.coffeeshopapi.telegram.service.TelegramUpdateHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${telegram.webhook-path:/api/telegram/webhook}")
public class TelegramWebhookController {

    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final TelegramUpdateHandler updateHandler;
    private final TelegramProperties properties;

    public TelegramWebhookController(TelegramUpdateHandler updateHandler, TelegramProperties properties) {
        this.updateHandler = updateHandler;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<Void> receiveUpdate(@RequestBody TelegramUpdate update,
                                              HttpServletRequest request) {
        String incomingSecret = request.getHeader(SECRET_HEADER);
        if (incomingSecret == null || !incomingSecret.equals(properties.getWebhookSecret())) {
            throw new InvalidWebhookSecretException();
        }

        updateHandler.handle(update);
        return ResponseEntity.ok().build();
    }
}
