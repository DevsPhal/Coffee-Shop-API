package org.group1.coffeeshopapi.inventory.service.impl;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.group1.coffeeshopapi.inventory.entity.StockExpense;
import org.group1.coffeeshopapi.inventory.repository.StockExpenseRepository;
import org.group1.coffeeshopapi.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Covers the generated Excel report's shape — header row, one row per expense, and a correctly
 * summed total row — untested before. See StockExpenseReportServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class StockExpenseReportServiceImplTest {

    @Mock private StockExpenseRepository stockExpenseRepository;
    @InjectMocks private StockExpenseReportServiceImpl service;

    @Test
    void reportListsEveryExpenseAndSumsTheirAmountsIntoATotalRow() throws IOException {
        when(stockExpenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(any(), any()))
                .thenReturn(List.of(
                        expense("Green Tea", "10.000", "1.50", LocalDate.of(2026, 3, 2)),
                        expense("Milk", "5.000", "2.00", LocalDate.of(2026, 3, 15))));

        byte[] report = service.generateMonthlyReport(YearMonth.of(2026, 3));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(report))) {
            Sheet sheet = workbook.getSheetAt(0);

            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Date");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Product");

            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Green Tea");
            assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Milk");

            Row totalRow = sheet.getRow(4);
            assertThat(totalRow.getCell(3).getStringCellValue()).isEqualTo("Total");
            // 10 * 1.50 + 5 * 2.00 = 15.00 + 10.00 = 25.00
            assertThat(totalRow.getCell(4).getNumericCellValue()).isEqualTo(25.00);
        }
    }

    @Test
    void anEmptyMonthStillProducesAValidWorkbookWithAZeroTotal() throws IOException {
        when(stockExpenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(any(), any()))
                .thenReturn(List.of());

        byte[] report = service.generateMonthlyReport(YearMonth.of(2026, 4));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(report))) {
            Sheet sheet = workbook.getSheetAt(0);
            Row totalRow = sheet.getRow(2);
            assertThat(totalRow.getCell(4).getNumericCellValue()).isZero();
        }
    }

    private StockExpense expense(String productName, String quantity, String unitCost, LocalDate date) {
        Product product = new Product();
        product.setName(productName);

        StockExpense expense = new StockExpense();
        expense.setProduct(product);
        expense.setQuantity(new BigDecimal(quantity));
        expense.setUnitCost(new BigDecimal(unitCost));
        expense.setAmount(new BigDecimal(quantity).multiply(new BigDecimal(unitCost)));
        expense.setExpenseDate(date);
        return expense;
    }
}
