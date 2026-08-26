package org.group1.coffeeshopapi.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;

public record UpdateProductSizeOptionRequest(
        String name,

        @DecimalMin(value = "0.0", message = "Price add-on must not be negative")
        BigDecimal priceDelta,

        Integer sortOrder,
        Status status
) {
}
