package org.group1.coffeeshopapi.common.exception;

import org.springframework.http.HttpStatus;

public class PaymentVerificationUnavailableException extends ApiException {
    private static final long serialVersionUID = 1L;

    public PaymentVerificationUnavailableException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }
}
