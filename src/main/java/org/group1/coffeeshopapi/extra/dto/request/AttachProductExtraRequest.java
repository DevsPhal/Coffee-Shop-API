package org.group1.coffeeshopapi.extra.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;
import jakarta.validation.constraints.Min;

public record AttachProductExtraRequest(
        @NotNull(message = "Extra is required")
        UUID extraId,

        @Min(value = 0, message = "Sort order must not be negative")
        Integer sortOrder
) {
}
