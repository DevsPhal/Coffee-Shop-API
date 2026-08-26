package org.group1.coffeeshopapi.product.dto.response;

import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Customer-facing menu projection of {@link ProductResponse} — deliberately excludes internal
 * fields (inventory counts, reorder thresholds, and staff audit identities) that {@code
 * ProductResponse} carries for the admin catalog view but that a customer has no business seeing.
 */
public record CustomerProductResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        String sku,
        String unit,
        BigDecimal price,
        UUID categoryId,
        String categoryName,
        Status status,
        DiscountType discountType,
        BigDecimal discountValue,
        LocalDateTime discountStartAt,
        LocalDateTime discountEndAt,
        boolean discountActive,
        BigDecimal finalPrice,
        List<ProductSizeOptionResponse> sizeOptions
) {
}
