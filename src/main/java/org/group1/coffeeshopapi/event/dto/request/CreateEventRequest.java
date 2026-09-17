package org.group1.coffeeshopapi.event.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// latitude/longitude must be given together, or omitted when the event has no fixed venue.
public record CreateEventRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
        BigDecimal latitude,

        @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
        BigDecimal longitude,

        @NotNull(message = "Start date/time is required")
        LocalDateTime startAt,

        @NotNull(message = "End date/time is required")
        LocalDateTime endAt
) {
}