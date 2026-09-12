package org.group1.coffeeshopapi.inventory.service;

import java.time.YearMonth;

public interface StockExpenseReportService {

    // Every stock-purchase expense (see InventoryServiceImpl.recordStockPurchaseExpense) recorded
    // in the given month, as an .xlsx workbook.
    byte[] generateMonthlyReport(YearMonth month);
}
