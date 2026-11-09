package org.group1.coffeeshopapi.inventory.service.impl;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.ReportPeriod;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.WasteReason;
import org.group1.coffeeshopapi.common.excel.BrandedSheet;
import org.group1.coffeeshopapi.common.excel.BrandingAssertions;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.inventory.repository.StockBatchRepository;
import org.group1.coffeeshopapi.inventory.repository.StockExpenseRepository;
import org.group1.coffeeshopapi.inventory.repository.StockMovementRepository;
import org.group1.coffeeshopapi.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.group1.coffeeshopapi.common.excel.BrandingAssertions.rowStartingWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryReportExportTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private StockExpenseRepository stockExpenseRepository;
    @Mock private StockBatchRepository stockBatchRepository;
    @InjectMocks private InventoryReportServiceImpl service;

    @Test
    void theInventoryExportIsBrandedOnEverySheetAndKeepsEveryFigure() throws IOException {
        Product latte = product("Iced Latte");
        Inventory inventory = new Inventory();
        inventory.setProduct(latte);
        inventory.setQuantityOnHand(new BigDecimal("12"));
        inventory.setReorderLevel(new BigDecimal("5"));
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory));
        when(stockMovementRepository.totalsBetween(any(), any())).thenReturn(List.of());
        when(stockMovementRepository.wasteBetween(any(), any())).thenReturn(List.of(waste(WasteReason.EXPIRED, "1.50")));
        when(stockExpenseRepository.findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(any(), any()))
                .thenReturn(List.of());
        when(stockBatchRepository.valueOnHand()).thenReturn(List.of(value(latte.getId(), "18.00")));

        byte[] xlsx = service.exportSummary(ReportPeriod.DAY, LocalDate.of(2026, 11, 9));

        try (XSSFWorkbook workbook = BrandingAssertions.open(xlsx)) {
            BrandingAssertions.assertEverySheetBranded(workbook);

            Sheet summary = workbook.getSheet("Summary");
            assertThat(BrandingAssertions.text(summary, BrandedSheet.TITLE_ROW)).isEqualTo("Inventory Report");
            assertThat(BrandingAssertions.text(summary, BrandedSheet.GENERATED_ROW + 1))
                    .isEqualTo("Period: Daily, 9 Nov 2026");
            assertThat(rowStartingWith(summary, "Stock value now").getCell(1).getNumericCellValue()).isEqualTo(18.0);
            assertThat(rowStartingWith(summary, "Waste").getCell(1).getNumericCellValue()).isZero();
            assertThat(rowStartingWith(summary, "Expired").getCell(1).getNumericCellValue()).isEqualTo(1.5);
            assertThat(rowStartingWith(summary, "Low stock products").getCell(1).getNumericCellValue()).isZero();

            Sheet products = workbook.getSheet("Products");
            Row header = rowStartingWith(products, "Product");
            assertThat(header.getCell(15).getStringCellValue()).isEqualTo("Stock Value Now");
            Row latteRow = products.getRow(header.getRowNum() + 1);
            assertThat(latteRow.getCell(0).getStringCellValue()).isEqualTo("Iced Latte");
            assertThat(latteRow.getCell(3).getStringCellValue()).isEqualTo("CUP");
            assertThat(latteRow.getCell(13).getNumericCellValue()).isEqualTo(12.0);
            assertThat(latteRow.getCell(13).getCellStyle().getDataFormatString()).isEqualTo("#,##0");
            assertThat(latteRow.getCell(15).getCellStyle().getDataFormatString()).contains("$#,##0.00");
            Row total = products.getRow(header.getRowNum() + 2);
            assertThat(total.getCell(0).getStringCellValue()).isEqualTo("Total");
            assertThat(total.getCell(15).getNumericCellValue()).isEqualTo(18.0);
            // Sixteen columns print across a landscape page.
            assertThat(products.getPrintSetup().getLandscape()).isTrue();
            assertThat(summary.getPrintSetup().getLandscape()).isFalse();
        }
    }

    private static Product product(String name) {
        Category category = new Category();
        category.setName("Coffee");
        category.setCategoryGroup(CategoryGroup.FRESH_DRINK);
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setCategory(category);
        product.setSellUnit(SellUnit.CUP);
        return product;
    }

    private static StockMovementRepository.WasteTotals waste(WasteReason reason, String cost) {
        return new StockMovementRepository.WasteTotals() {
            @Override
            public WasteReason getWasteReason() {
                return reason;
            }

            @Override
            public BigDecimal getTotalCost() {
                return new BigDecimal(cost);
            }
        };
    }

    private static StockBatchRepository.StockValue value(UUID productId, String value) {
        return new StockBatchRepository.StockValue() {
            @Override
            public UUID getProductId() {
                return productId;
            }

            @Override
            public BigDecimal getQuantity() {
                return BigDecimal.ONE;
            }

            @Override
            public BigDecimal getValue() {
                return new BigDecimal(value);
            }
        };
    }
}
