package org.group1.coffeeshopapi.common.exception;

import org.springframework.http.HttpStatus;

public class FileStorageException extends ApiException {
    private static final long serialVersionUID = 1L;

    public FileStorageException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
