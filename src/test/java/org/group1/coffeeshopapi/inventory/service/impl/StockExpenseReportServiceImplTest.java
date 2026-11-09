package org.group1.coffeeshopapi.inventory.service.impl;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.common.excel.BrandedSheet;
import org.group1.coffeeshopapi.common.excel.BrandingAssertions;
import org.group1.coffeeshopapi.inventory.entity.StockExpense;
import org.group1.coffeeshopapi.inventory.repository.StockExpenseRepository;
import org.group1.coffeeshopapi.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockExpenseReportServiceImplTest {

    @Mock private StockExpenseRepository stockExpenseRepository;
    @InjectMocks private StockExpenseReportServiceImpl service;

    @Test
    void reportListsEveryExpenseAndSumsTheirAmountsIntoATotalRow() throws IOException {
        when(stockExpenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(any(), any()))
                .thenReturn(List.of(
                        expense("Green Tea", "10.000", "1.50", LocalDate.of(2026, 3, 2), "CAN"),
                        expense("Milk", "5.000", "2.00", LocalDate.of(2026, 3, 15), null)));

        byte[] report = service.generateMonthlyReport(YearMonth.of(2026, 3));

        try (XSSFWorkbook workbook = BrandingAssertions.open(report)) {
            BrandingAssertions.assertEverySheetBranded(workbook);
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("Stock Expenses 2026-03");
            assertThat(BrandingAssertions.text(sheet, BrandedSheet.TITLE_ROW)).isEqualTo("Stock Expenses");
            assertThat(BrandingAssertions.text(sheet, BrandedSheet.GENERATED_ROW + 1)).isEqualTo("Month: March 2026");

            Row header = BrandingAssertions.rowStartingWith(sheet, "Date");
            int first = header.getRowNum() + 1;
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Product");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("Unit");

            assertThat(sheet.getRow(first).getCell(1).getStringCellValue()).isEqualTo("Green Tea");
            assertThat(sheet.getRow(first + 1).getCell(1).getStringCellValue()).isEqualTo("Milk");
            assertThat(sheet.getRow(first).getCell(3).getStringCellValue()).isEqualTo("CAN");
            // Recorded before expenses kept a unit: those were always in the product's stock unit.
            assertThat(sheet.getRow(first + 1).getCell(3).getStringCellValue()).isEqualTo("PACK");
            assertThat(sheet.getRow(first).getCell(4).getNumericCellValue()).isEqualTo(1.50);
            assertThat(sheet.getRow(first).getCell(4).getCellStyle().getDataFormatString()).contains("$#,##0.00");

            Row totalRow = sheet.getRow(first + 2);
            assertThat(totalRow.getCell(0).getStringCellValue()).isEqualTo("Total");
            assertThat(totalRow.getCell(5).getNumericCellValue()).isEqualTo(25.00);
            assertThat(workbook.getFontAt(totalRow.getCell(5).getCellStyle().getFontIndex()).getBold()).isTrue();
            // The header row repeats on every printed page, and the screen keeps it in view.
            assertThat(sheet.getRepeatingRows().getFirstRow()).isEqualTo(header.getRowNum());
            assertThat(sheet.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) first);
        }
    }

    @Test
    void anEmptyMonthStillProducesAValidWorkbookWithAZeroTotal() throws IOException {
        when(stockExpenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(any(), any()))
                .thenReturn(List.of());

        byte[] report = service.generateMonthlyReport(YearMonth.of(2026, 4));

        try (XSSFWorkbook workbook = BrandingAssertions.open(report)) {
            BrandingAssertions.assertEverySheetBranded(workbook);
            Sheet sheet = workbook.getSheetAt(0);
            Row totalRow = BrandingAssertions.rowStartingWith(sheet, "Total");
            assertThat(totalRow.getRowNum()).isEqualTo(BrandingAssertions.rowStartingWith(sheet, "Date").getRowNum() + 1);
            assertThat(totalRow.getCell(5).getNumericCellValue()).isZero();
        }
    }

    private StockExpense expense(String productName, String quantity, String unitCost, LocalDate date, String unit) {
        Product product = new Product();
        product.setName(productName);
        product.setStockUnit(StockUnit.PACK);

        StockExpense expense = new StockExpense();
        expense.setProduct(product);
        expense.setQuantity(new BigDecimal(quantity));
        expense.setUnit(unit);
        expense.setUnitCost(new BigDecimal(unitCost));
        expense.setAmount(new BigDecimal(quantity).multiply(new BigDecimal(unitCost)));
        expense.setExpenseDate(date);
        return expense;
    }
}
