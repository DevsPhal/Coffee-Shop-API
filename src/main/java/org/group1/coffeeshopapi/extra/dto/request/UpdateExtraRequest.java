package org.group1.coffeeshopapi.extra.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.Status;

import java.math.BigDecimal;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Digits;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;

public record UpdateExtraRequest(
        @Pattern(regexp = ValidationPatterns.NOT_BLANK_REGEX, message = "Name can't be blank")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        @DecimalMin(value = "0.0", message = "Price must not be negative")
        @Digits(integer = ValidationPatterns.MONEY_INTEGER_DIGITS, fraction = 2, message = "Price must be a valid amount with at most 2 decimal places")
        BigDecimal price,

        Status status,

        @DecimalMin(value = "0.0", message = "Quantity on hand must not be negative")
        BigDecimal quantityOnHand
) {
}
