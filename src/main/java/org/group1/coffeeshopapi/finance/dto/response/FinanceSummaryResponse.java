package org.group1.coffeeshopapi.finance.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinanceSummaryResponse(
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal cashIn,
        BigDecimal bakongIn,
        BigDecimal totalIn,

        // The two streams totalOut adds together — broken out so "why is totalOut not zero" is
        // never a mystery: stockPurchasesOut is never a manual entry, it's auto-recorded the
        // moment stock is restocked (see InventoryServiceImpl.recordStockPurchaseExpense).
        BigDecimal manualExpensesOut,
        BigDecimal stockPurchasesOut,

        BigDecimal totalOut,
        BigDecimal profit
) {
}
