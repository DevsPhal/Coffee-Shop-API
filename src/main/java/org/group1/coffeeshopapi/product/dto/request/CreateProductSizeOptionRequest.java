package org.group1.coffeeshopapi.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductSizeOptionRequest(
        @NotBlank(message = "Size name is required")
        String name,

        @NotNull(message = "Price add-on is required")
        @DecimalMin(value = "0.0", message = "Price add-on must not be negative")
        BigDecimal priceDelta,

        Integer sortOrder
) {
}
