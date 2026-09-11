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
        String description,
        String imageUrl,
        String sku,
        StockUnit stockUnit,
        SellUnit sellUnit,
        BigDecimal unitsPerStock,
        UUID categoryId,
        String categoryName,
        // Which cart/order customizations this product accepts (size/sugar/ice/milk vs. size-only
        // vs. none), inherited from its category — see ProductVariantPolicy.
        CategoryGroup categoryGroup,
        Status status,
        BigDecimal quantityOnHand,
        BigDecimal reorderLevel,
        DiscountType discountType,
        BigDecimal discountValue,
        LocalDateTime discountStartAt,
        LocalDateTime discountEndAt,
        boolean discountActive,
        // A product has no price of its own — each option below carries its own price/finalPrice
        // (see ProductPriceResolver). Empty means the product isn't purchasable yet.
        List<ProductSizeOptionResponse> sizeOptions,
        // Extras (e.g. Pearl) this product offers — the customer chooses to add or not add each
        // one at cart time. Empty means no extras are offered on this product.
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