package org.group1.coffeeshopapi.event.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotNull(message = "Start date/time is required")
        LocalDateTime startAt,

        @NotNull(message = "End date/time is required")
        LocalDateTime endAt
) {
}