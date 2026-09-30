package org.group1.coffeeshopapi.finance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinanceSummaryResponse(
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal cashIn,
        BigDecimal bakongIn,
        BigDecimal totalIn,

        // The two streams that add up to totalOut — manual entries vs. auto-recorded stock costs.
        BigDecimal manualExpensesOut,
        BigDecimal stockPurchasesOut,

        BigDecimal totalOut,
        BigDecimal profit
) {
}
