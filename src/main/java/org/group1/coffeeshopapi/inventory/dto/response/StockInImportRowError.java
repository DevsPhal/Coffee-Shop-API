package org.group1.coffeeshopapi.inventory.dto.response;

public record StockInImportRowError(
        int rowNumber,
        String sku,
        String message
) {
}
