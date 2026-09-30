package org.group1.coffeeshopapi.finance.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateExpenseRequest(
        @Size(max = 255, message = "Category must not exceed 255 characters")
        String category,
        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
        BigDecimal amount,

        LocalDate expenseDate
) {
}
