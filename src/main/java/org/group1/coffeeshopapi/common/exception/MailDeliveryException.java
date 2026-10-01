package org.group1.coffeeshopapi.common.exception;

import org.springframework.http.HttpStatus;

public class MailDeliveryException extends ApiException {
    private static final long serialVersionUID = 1L;

    public MailDeliveryException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
