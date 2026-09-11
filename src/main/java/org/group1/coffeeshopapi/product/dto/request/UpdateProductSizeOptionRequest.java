package org.group1.coffeeshopapi.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;

public record UpdateProductSizeOptionRequest(
        String name,

        @DecimalMin(value = "0.0", message = "Price must not be negative")
        BigDecimal price,

        Integer sortOrder,
        Status status
) {
}
