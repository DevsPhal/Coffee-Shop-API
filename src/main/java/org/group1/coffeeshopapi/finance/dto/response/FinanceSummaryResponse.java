package org.group1.coffeeshopapi.finance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinanceSummaryResponse(
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal cashIn,
        BigDecimal bakongIn,
        BigDecimal totalIn,
        BigDecimal totalOut,
        BigDecimal profit
) {
}
