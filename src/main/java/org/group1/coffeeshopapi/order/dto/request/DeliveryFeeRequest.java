package org.group1.coffeeshopapi.order.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;

public record DeliveryFeeRequest(
        @NotNull(message = "Delivery fee is required")
        @DecimalMin(value = "0.0", message = "Delivery fee must not be negative")
        @Digits(integer = ValidationPatterns.MONEY_INTEGER_DIGITS, fraction = 2, message = "Delivery fee must be a valid amount with at most 2 decimal places")
        BigDecimal fee
) {
}
