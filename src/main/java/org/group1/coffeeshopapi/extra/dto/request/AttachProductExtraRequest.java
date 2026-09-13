package org.group1.coffeeshopapi.extra.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttachProductExtraRequest(
        @NotNull(message = "Extra is required")
        UUID extraId,

        Integer sortOrder
) {
}
