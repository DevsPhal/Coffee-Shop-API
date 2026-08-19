package org.group1.coffeeshopapi.telegram.exception;

import org.group1.coffeeshopapi.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidWebhookSecretException extends ApiException {
    public InvalidWebhookSecretException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}