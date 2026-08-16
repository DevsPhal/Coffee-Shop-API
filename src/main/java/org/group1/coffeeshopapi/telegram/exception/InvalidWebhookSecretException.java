package org.group1.coffeeshopapi.telegram.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class InvalidWebhookSecretException extends RuntimeException {
    public InvalidWebhookSecretException() {
        super("Invalid Telegram webhook secret token");
    }
}
