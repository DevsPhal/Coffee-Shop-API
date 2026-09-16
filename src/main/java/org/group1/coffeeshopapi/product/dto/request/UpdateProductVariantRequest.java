package org.group1.coffeeshopapi.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.VariantLabel;

import java.math.BigDecimal;

public record UpdateProductVariantRequest(
        VariantLabel name,

        @DecimalMin(value = "0.0", message = "Price must not be negative")
        BigDecimal price,

        Integer sortOrder,
        Status status
) {
}
