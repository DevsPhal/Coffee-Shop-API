package org.group1.coffeeshopapi.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidOtpException extends ApiException {
    private static final long serialVersionUID = 1L;

    public InvalidOtpException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}