package org.group1.coffeeshopapi.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.StockUnit;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @Size(max = 255, message = "Khmer name must not exceed 255 characters")
        String nameKh,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotBlank(message = "SKU is required")
        @Size(max = 255, message = "SKU must not exceed 255 characters")
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
