package org.group1.coffeeshopapi.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

// The one error shape every endpoint returns. `errors` is present only when specific request fields are
// invalid, so a form can show each message next to its own input; `message` always sums the problem up.
@Getter
@Builder
public class ErrorResponse {
    private HttpStatus status;
    private String message;
    private String path;
    @Builder.Default
    private LocalDateTime timeStamp = LocalDateTime.now();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<FieldMessage> errors;

    // `field` is the request property path, e.g. "fullName" or "items[0].quantity".
    public record FieldMessage(String field, String message) {
    }
}
