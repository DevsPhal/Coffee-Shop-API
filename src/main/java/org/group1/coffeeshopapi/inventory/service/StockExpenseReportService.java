package org.group1.coffeeshopapi.inventory.service;

import java.time.YearMonth;

public interface StockExpenseReportService {

    byte[] generateMonthlyReport(YearMonth month);
}
