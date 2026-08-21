package org.group1.coffeeshopapi.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank(message = "Product name is required")
        String name,

        String description,

        @NotBlank(message = "SKU is required")
        String sku,

        @NotBlank(message = "Unit is required")
        String unit,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", message = "Price must not be negative")
        BigDecimal price,

        @NotNull(message = "Category is required")
        UUID categoryId,

        @DecimalMin(value = "0.0", message = "Reorder level must not be negative")
        BigDecimal reorderLevel
) {
}
