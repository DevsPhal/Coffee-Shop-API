package org.group1.coffeeshopapi.extra.dto.request;

import org.group1.coffeeshopapi.common.enums.Status;
import jakarta.validation.constraints.Min;

public record UpdateProductExtraRequest(
        @Min(value = 0, message = "Sort order must not be negative")
        Integer sortOrder,
        Status status
) {
}
