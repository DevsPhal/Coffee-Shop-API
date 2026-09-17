package org.group1.coffeeshopapi.inventory.service;

import java.time.YearMonth;

public interface StockExpenseReportService {

    // Every stock-purchase expense recorded in the given month, as an .xlsx workbook.
    byte[] generateMonthlyReport(YearMonth month);
}
