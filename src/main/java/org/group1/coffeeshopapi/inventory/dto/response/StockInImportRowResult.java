package org.group1.coffeeshopapi.inventory.dto.response;

import org.group1.coffeeshopapi.common.enums.SellUnit;
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
        // What the row added to stock, e.g. 2 CARTON received is 48 CAN.
        BigDecimal baseQuantity,
        SellUnit sellUnit,
        BigDecimal unitCost,
        BigDecimal amount
) {
}
