package org.group1.coffeeshopapi.inventory.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record StockInImportResponse(
        int totalRows,
        int created,
        int failed,
        BigDecimal totalCost,
        List<StockInImportRowResult> received,
        List<StockInImportRowError> errors
) {
}
