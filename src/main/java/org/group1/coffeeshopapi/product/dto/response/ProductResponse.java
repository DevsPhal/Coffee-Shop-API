package org.group1.coffeeshopapi.product.dto.response;

import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
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
        BigDecimal quantityOnHand,
        BigDecimal reorderLevel,
        DiscountType discountType,
        BigDecimal discountValue,
        LocalDateTime discountStartAt,
        LocalDateTime discountEndAt,
        boolean discountActive,
        BigDecimal finalPrice,
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