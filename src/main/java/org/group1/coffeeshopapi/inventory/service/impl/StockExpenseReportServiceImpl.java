package org.group1.coffeeshopapi.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.excel.BrandedSheet;
import org.group1.coffeeshopapi.common.excel.BrandedWorkbook;
import org.group1.coffeeshopapi.common.excel.ExcelText;
import org.group1.coffeeshopapi.common.excel.TableColumn;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.inventory.entity.StockExpense;
import org.group1.coffeeshopapi.inventory.repository.StockExpenseRepository;
import org.group1.coffeeshopapi.inventory.service.StockExpenseReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockExpenseReportServiceImpl implements StockExpenseReportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<TableColumn> COLUMNS = List.of(
            TableColumn.text("Date"), TableColumn.text("Product"), TableColumn.quantity("Quantity"),
            TableColumn.text("Unit"), TableColumn.money("Unit Cost"), TableColumn.money("Amount"));

    private final StockExpenseRepository stockExpenseRepository;

    @Override
    public byte[] generateMonthlyReport(YearMonth month) {
        List<StockExpense> expenses = stockExpenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(
                month.atDay(1), month.plusMonths(1).atDay(1));

        List<Object[]> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (StockExpense expense : expenses) {
            rows.add(new Object[] {expense.getExpenseDate().format(DATE_FORMAT), expense.getProduct().getName(),
                    expense.getQuantity(), unitOf(expense), expense.getUnitCost(), expense.getAmount()});
            total = total.add(expense.getAmount());
        }

        try (BrandedWorkbook book = new BrandedWorkbook()) {
            BrandedSheet sheet = book.sheet("Stock Expenses " + month, "Stock Expenses", List.of(
                    "Month: " + ExcelText.month(month),
                    "Records: " + expenses.size() + " · Total spent: " + ExcelText.usd(total)));
            sheet.table(COLUMNS, rows, new Object[] {"Total", null, null, null, null, total});
            return book.toBytes();
        } catch (IOException e) {
            throw new InvalidOperationException("Unable to generate expense report: " + e.getMessage());
        }
    }

    // Expenses recorded before units were kept were all in the product's stock unit.
    private static String unitOf(StockExpense expense) {
        if (expense.getUnit() != null) {
            return expense.getUnit();
        }
        return expense.getProduct().getStockUnit() != null ? expense.getProduct().getStockUnit().name() : "";
    }
}
