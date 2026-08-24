package org.group1.coffeeshopapi.finance.dto.request;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateExpenseRequest(
        String category,
        String description,

        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
        BigDecimal amount,

        LocalDate expenseDate
) {
}
