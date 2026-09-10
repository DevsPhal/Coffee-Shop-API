package org.group1.coffeeshopapi.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        BigDecimal reorderLevel,

        // Optional launch discount, e.g. a new product that goes on the menu at 10% off. A null
        // discountValue means the product starts at full price; the discount can still be set
        // later through PUT /{id}/discount. A percentage is the common case, so a value sent
        // without an explicit discountType is treated as one.
        DiscountType discountType,

        @DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be greater than zero")
        BigDecimal discountValue,

        LocalDateTime discountStartAt,
        LocalDateTime discountEndAt
) {
}
