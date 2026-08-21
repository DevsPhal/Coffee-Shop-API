package org.group1.coffeeshopapi.finance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        String category,
        String description,
        BigDecimal amount,
        LocalDate expenseDate,
        UUID recordedBy,
        LocalDateTime createdAt
) {
}
