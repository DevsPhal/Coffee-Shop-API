package org.group1.coffeeshopapi.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.VariantLabel;

import java.math.BigDecimal;

public record CreateProductVariantRequest(
        @NotNull(message = "Variant name is required")
        VariantLabel name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", message = "Price must not be negative")
        BigDecimal price,

        // Base units one sale takes from stock. Blank = 1, or the whole stock unit for a matching pack label.
        @Min(value = 1, message = "Units per sale must be at least 1")
        Integer unitsPerSale,

        Integer sortOrder
) {
}
