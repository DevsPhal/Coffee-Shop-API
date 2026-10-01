package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EstimatedTimeRequest(
        @NotNull(message = "Estimated minutes are required")
        @Min(value = 1, message = "Estimated time must be at least 1 minute")
        @Max(value = 240, message = "Estimated time must not exceed 240 minutes")
        Integer minutes
) {
}
