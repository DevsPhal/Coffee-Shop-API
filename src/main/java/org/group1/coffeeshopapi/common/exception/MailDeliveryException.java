package org.group1.coffeeshopapi.common.exception;

import org.springframework.http.HttpStatus;

// SMTP genuinely failed to send (bad credentials, unreachable host, connection timeout, ...) — see
// MailServiceImpl#sendOtpEmail. 503 rather than 500: the API itself is fine, an external dependency
// (the mail provider) isn't, and the client's best move is to retry shortly, same as any other
// "service temporarily unavailable" case.
public class MailDeliveryException extends ApiException {
    private static final long serialVersionUID = 1L;

    public MailDeliveryException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message, cause);
    }
}
