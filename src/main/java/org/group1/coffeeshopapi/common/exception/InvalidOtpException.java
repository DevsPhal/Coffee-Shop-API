package org.group1.coffeeshopapi.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidOtpException extends ApiException {
    public InvalidOtpException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}