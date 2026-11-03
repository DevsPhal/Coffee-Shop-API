package org.group1.coffeeshopapi.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.VariantLabel;

import java.math.BigDecimal;

public record UpdateProductVariantRequest(
        VariantLabel name,

        @DecimalMin(value = "0.0", message = "Price must not be negative")
        BigDecimal price,

        @Min(value = 1, message = "Units per sale must be at least 1")
        Integer unitsPerSale,

        Integer sortOrder,
        Status status
) {
}
