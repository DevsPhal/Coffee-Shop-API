package org.group1.coffeeshopapi.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SetProductDiscountRequest(
        @NotNull(message = "Discount type is required")
        DiscountType discountType,

        @NotNull(message = "Discount value is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be greater than zero")
        BigDecimal discountValue,

        LocalDateTime discountStartAt,
        LocalDateTime discountEndAt
) {
}