package org.group1.coffeeshopapi.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.StockUnit;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
        String name,
        String description,
        String sku,
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