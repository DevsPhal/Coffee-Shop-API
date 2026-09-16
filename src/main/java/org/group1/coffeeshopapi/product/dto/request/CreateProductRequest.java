package org.group1.coffeeshopapi.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.StockUnit;

import java.math.BigDecimal;
import java.util.UUID;

// No price here — a product has no price of its own; add one or more variants via
// ProductVariantService after creating it (see ProductPriceResolver).
public record CreateProductRequest(
        @NotBlank(message = "Product name is required")
        String name,

        String description,

        @NotBlank(message = "SKU is required")
        String sku,

        @NotNull(message = "Stock unit is required")
        StockUnit stockUnit,

        @NotNull(message = "Sell unit is required")
        SellUnit sellUnit,

        @DecimalMin(value = "0.001", message = "Units per stock must be positive")
        BigDecimal unitsPerStock,

        @NotNull(message = "Category is required")
        UUID categoryId,

        @DecimalMin(value = "0.0", message = "Reorder level must not be negative")
        BigDecimal reorderLevel
) {
}
