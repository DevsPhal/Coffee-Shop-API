package org.group1.coffeeshopapi.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.SkuMode;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.StockUnit;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,
        @Size(max = 255, message = "Khmer name must not exceed 255 characters")
        String nameKh,
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,
        @Size(max = ValidationPatterns.SKU_MAX_LENGTH, message = "SKU must not exceed 64 characters")
        @Pattern(regexp = ValidationPatterns.SKU_REGEX, message = ValidationPatterns.SKU_MESSAGE)
        String sku,
        SkuMode skuMode,
        StockUnit stockUnit,
        SellUnit sellUnit,

        @DecimalMin(value = "0.001", message = "Units per stock must be positive")
        BigDecimal unitsPerStock,

        UUID categoryId,
        Status status,

        @DecimalMin(value = "0.0", message = "Reorder level must not be negative")
        BigDecimal reorderLevel
) {
}
