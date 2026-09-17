package org.group1.coffeeshopapi.common.exception;

import org.springframework.http.HttpStatus;

// The mail server failed to send — 503, since it's the mail provider's fault, not ours.
public class MailDeliveryException extends ApiException {
    private static final long serialVersionUID = 1L;

    public MailDeliveryException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
