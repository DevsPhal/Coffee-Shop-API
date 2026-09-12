package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DeliveryFeeRequest(
        @NotNull(message = "Delivery fee is required")
        @DecimalMin(value = "0.0", message = "Delivery fee must not be negative")
        BigDecimal fee
) {
}
