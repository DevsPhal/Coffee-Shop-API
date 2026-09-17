package org.group1.coffeeshopapi.extra.dto.request;

import jakarta.validation.constraints.DecimalMin;
import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;

// Null fields are left unchanged. quantityOnHand is also how an admin restocks an extra.
public record UpdateExtraRequest(
        String name,

        @DecimalMin(value = "0.0", message = "Price must not be negative")
        BigDecimal price,

        Status status,

        @DecimalMin(value = "0.0", message = "Quantity on hand must not be negative")
        BigDecimal quantityOnHand
) {
}
