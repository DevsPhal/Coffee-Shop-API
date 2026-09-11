package org.group1.coffeeshopapi.inventory.dto.response;

import java.util.List;

public record StockInImportResponse(
        int totalRows,
        int created,
        int failed,
        List<StockInImportRowError> errors
) {
}
