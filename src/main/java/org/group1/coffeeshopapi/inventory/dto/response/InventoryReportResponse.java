package org.group1.coffeeshopapi.inventory.dto.response;

import org.group1.coffeeshopapi.common.enums.ReportPeriod;
import org.group1.coffeeshopapi.common.enums.WasteReason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record InventoryReportResponse(
        ReportPeriod period,
        LocalDate from,
        // Inclusive: the last day in the period.
        LocalDate to,
        BigDecimal purchaseCost,
        long purchaseCount,
        // Cost of goods sold, at FIFO batch cost.
        BigDecimal salesCost,
        long saleCount,
        BigDecimal wasteCost,
        long wasteCount,
        Map<WasteReason, BigDecimal> wasteCostByReason,
        BigDecimal returnValue,
        BigDecimal adjustmentValue,
        long movementCount,
        // Stock on hand right now, valued at batch cost.
        BigDecimal stockValue,
        long lowStockCount,
        long outOfStockCount,
        List<InventoryReportRow> products
) {
}
