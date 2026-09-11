package org.group1.coffeeshopapi.common.exception;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends ApiException {
    private static final long serialVersionUID = 1L;

    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}