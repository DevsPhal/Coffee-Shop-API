package org.group1.coffeeshopapi.inventory.service.impl;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.group1.coffeeshopapi.common.enums.StockUnit;
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

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(report))) {
            Sheet sheet = workbook.getSheetAt(0);

            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Date");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Product");

            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Green Tea");
            assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Milk");
            assertThat(header.getCell(3).getStringCellValue()).isEqualTo("Unit");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("CAN");
            // Recorded before expenses kept a unit: those were always in the product's stock unit.
            assertThat(sheet.getRow(2).getCell(3).getStringCellValue()).isEqualTo("PACK");

            Row totalRow = sheet.getRow(4);
            assertThat(totalRow.getCell(4).getStringCellValue()).isEqualTo("Total");
            assertThat(totalRow.getCell(5).getNumericCellValue()).isEqualTo(25.00);
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
