package org.group1.coffeeshopapi.telegram.exception;

import lombok.Getter;
import org.group1.coffeeshopapi.common.exception.ApiException;
import org.springframework.http.HttpStatus;

@Getter
public class TelegramDeliveryException extends ApiException {
    private static final long serialVersionUID = 1L;

    public enum Reason {
        NOT_CONFIGURED,
        CHAT_UNREACHABLE,
        UNAVAILABLE
    }

    private final Reason reason;

    public TelegramDeliveryException(Reason reason, String message) {
        super(reason == Reason.CHAT_UNREACHABLE ? HttpStatus.CONFLICT : HttpStatus.SERVICE_UNAVAILABLE, message);
        this.reason = reason;
    }
}
