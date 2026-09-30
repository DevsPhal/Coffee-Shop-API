package org.group1.coffeeshopapi.common.exception;

import org.springframework.http.HttpStatus;

// The bank couldn't be asked whether a payment arrived. Distinct from "not paid yet": answering
// that instead would leave a paid order stuck as unpaid with nothing pointing at the real fault.
public class PaymentVerificationUnavailableException extends ApiException {
    private static final long serialVersionUID = 1L;

    public PaymentVerificationUnavailableException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }
}
