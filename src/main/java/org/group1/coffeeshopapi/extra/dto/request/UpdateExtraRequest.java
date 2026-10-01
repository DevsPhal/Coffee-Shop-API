package org.group1.coffeeshopapi.extra.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;

public record UpdateExtraRequest(
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @DecimalMin(value = "0.0", message = "Price must not be negative")
        BigDecimal price,

        Status status,

        @DecimalMin(value = "0.0", message = "Quantity on hand must not be negative")
        BigDecimal quantityOnHand
) {
}
