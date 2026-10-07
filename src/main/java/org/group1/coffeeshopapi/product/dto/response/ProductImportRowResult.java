package org.group1.coffeeshopapi.product.dto.response;

import org.group1.coffeeshopapi.common.enums.SkuMode;

import java.util.UUID;

public record ProductImportRowResult(
        int rowNumber,
        UUID productId,
        String name,
        String sku,
        SkuMode skuMode
) {
}
