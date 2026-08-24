package org.group1.coffeeshopapi.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
        String name,
        String description,
        String unit,

        @DecimalMin(value = "0.0", message = "Price must not be negative")
        BigDecimal price,

        UUID categoryId,
        Status status,

        @DecimalMin(value = "0.0", message = "Reorder level must not be negative")
        BigDecimal reorderLevel
) {
}