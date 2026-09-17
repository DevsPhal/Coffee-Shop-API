package org.group1.coffeeshopapi.event.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// latitude/longitude must be given together, or omitted to leave the venue pin unchanged.
public record UpdateEventRequest(
        String title,
        String description,

        @DecimalMin(value = "-90", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90", message = "Latitude must be between -90 and 90")
        BigDecimal latitude,

        @DecimalMin(value = "-180", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180", message = "Longitude must be between -180 and 180")
        BigDecimal longitude,

        LocalDateTime startAt,
        LocalDateTime endAt,
        Status status
) {
}