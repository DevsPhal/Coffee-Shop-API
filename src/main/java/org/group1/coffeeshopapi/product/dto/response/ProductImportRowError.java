package org.group1.coffeeshopapi.product.dto.response;

public record ProductImportRowError(
        int rowNumber,
        String sku,
        String message
) {
}
