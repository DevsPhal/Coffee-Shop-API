package org.group1.coffeeshopapi.product.dto.response;

import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.extra.dto.response.ProductExtraResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Customer-facing menu projection of {@link ProductResponse} — deliberately excludes internal
 * fields (inventory counts, reorder thresholds, and staff audit identities) that {@code
 * ProductResponse} carries for the admin catalog view but that a customer has no business seeing.
 * categoryGroup is kept, unlike those — the app needs it to decide whether to show
 * size/sugar/ice/milk pickers, a size-only picker, or nothing at all for this product (see
 * ProductVariantPolicy).
 */
public record CustomerProductResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        String sku,
        StockUnit stockUnit,
        SellUnit sellUnit,
        BigDecimal unitsPerStock,
        UUID categoryId,
        String categoryName,
        CategoryGroup categoryGroup,
        Status status,
        DiscountType discountType,
        BigDecimal discountValue,
        LocalDateTime discountStartAt,
        LocalDateTime discountEndAt,
        boolean discountActive,
        List<ProductSizeOptionResponse> sizeOptions,
        // Extras (e.g. Pearl) this product offers — the customer chooses to add or not add each
        // one when ordering.
        List<ProductExtraResponse> extras
) {
}
