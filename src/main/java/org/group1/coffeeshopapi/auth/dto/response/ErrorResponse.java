package org.group1.coffeeshopapi.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private Map<String, String> error;
    private LocalDateTime timestamp;
}
