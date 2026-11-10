package org.group1.coffeeshopapi.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenOperationException extends ApiException {
    private static final long serialVersionUID = 1L;

    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
