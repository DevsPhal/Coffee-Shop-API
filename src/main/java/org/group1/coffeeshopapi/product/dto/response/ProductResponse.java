package org.group1.coffeeshopapi.product.dto.response;

import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.extra.dto.response.ProductExtraResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        // Khmer translation of the name — null if none was set.
        String nameKh,
        String description,
        String imageUrl,
        String sku,
        StockUnit stockUnit,
        SellUnit sellUnit,
        BigDecimal unitsPerStock,
        UUID categoryId,
        String categoryName,
        // Which customizations this product accepts, inherited from its category.
        CategoryGroup categoryGroup,
        Status status,
        BigDecimal quantityOnHand,
        BigDecimal reorderLevel,
        DiscountType discountType,
        BigDecimal discountValue,
        LocalDateTime discountStartAt,
        LocalDateTime discountEndAt,
        boolean discountActive,
        // Empty means the product isn't purchasable yet.
        List<ProductVariantResponse> variants,
        // Extras this product offers. Empty means none.
        List<ProductExtraResponse> extras,
        UUID createdBy,
        String createdByName,
        Role createdByRole,
        UUID updatedBy,
        String updatedByName,
        Role updatedByRole,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}