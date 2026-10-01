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

public record CustomerProductResponse(
        UUID id,
        String name,
        String nameKh,
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
        List<ProductVariantResponse> variants,
        List<ProductExtraResponse> extras
) {
}
