package org.group1.coffeeshopapi.inventory.dto.response;

import org.group1.coffeeshopapi.common.enums.StockUnit;

import java.math.BigDecimal;
import java.util.UUID;

public record StockInImportRowResult(
        int rowNumber,
        UUID productId,
        String sku,
        String productName,
        BigDecimal quantity,
        StockUnit stockUnit,
        BigDecimal unitCost,
        BigDecimal amount
) {
}
