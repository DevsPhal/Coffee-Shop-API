package org.group1.coffeeshopapi.common.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApiResponse<T> {
    private HttpStatus status;
    private String message;
    private T data;
    @Builder.Default
    private LocalDateTime timeStamp = LocalDateTime.now();

    public static <T> ApiResponse<T> of(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .build();
    }
}
