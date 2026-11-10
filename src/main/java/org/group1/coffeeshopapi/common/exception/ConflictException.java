package org.group1.coffeeshopapi.common.exception;

import org.springframework.http.HttpStatus;

// The request is valid but clashes with the current state (a table already taken, an overlapping booking...).
public class ConflictException extends ApiException {
    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
