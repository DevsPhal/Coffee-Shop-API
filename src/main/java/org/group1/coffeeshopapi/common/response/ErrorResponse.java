package org.group1.coffeeshopapi.common.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {
    private HttpStatus status;
    private String message;
    private String path;
    @Builder.Default
    private LocalDateTime timeStamp = LocalDateTime.now();
}
