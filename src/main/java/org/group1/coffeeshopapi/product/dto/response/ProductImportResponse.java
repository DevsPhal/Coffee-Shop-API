package org.group1.coffeeshopapi.product.dto.response;

import java.util.List;

public record ProductImportResponse(
        int totalRows,
        int created,
        int failed,
        List<ProductImportRowError> errors
) {
}
