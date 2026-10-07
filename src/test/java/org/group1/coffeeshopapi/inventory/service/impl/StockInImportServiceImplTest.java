package org.group1.coffeeshopapi.inventory.service.impl;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.inventory.dto.request.StockInRequest;
import org.group1.coffeeshopapi.inventory.dto.response.StockInImportResponse;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.excel.StockInImportColumn;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockInImportServiceImplTest {

    @Mock private InventoryService inventoryService;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private ProductRepository productRepository;
    @InjectMocks private StockInImportServiceImpl service;

    @Test
    void aBadImportRowIsReportedWithoutSinkingTheValidOnes() throws Exception {
        Product good = product("GOOD");
        Product orphan = product("ORPHAN");
        when(productRepository.findBySkuIgnoreCase("GOOD")).thenReturn(Optional.of(good));
        when(productRepository.findBySkuIgnoreCase("ORPHAN")).thenReturn(Optional.of(orphan));
        when(inventoryService.stockIn(argThat(request -> request != null && request.productId().equals(orphan.getId())), any()))
                .thenThrow(new ResourceNotFoundException("Inventory not found for product"));

        StockInImportResponse response = service.importFromExcel(legacyWorkbook(
                new String[] {"ORPHAN", "5", "1.00"}, new String[] {"GOOD", "10", "2.00"}), UUID.randomUUID());

        assertThat(response.created()).isEqualTo(1);
        assertThat(response.totalCost()).isEqualByComparingTo("20.00");
        assertThat(response.errors()).singleElement()
                .satisfies(error -> assertThat(error.message()).contains("Inventory not found"));
    }

    @Test
    void filledTemplateIgnoresTheProductColumnAndAcceptsVariantSkus() throws Exception {
        Product latte = product("FD-COF-IL-001");
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory(latte)));
        when(productRepository.findBySkuIgnoreCase("FD-COF-IL-001-M")).thenReturn(Optional.empty());
        when(productRepository.findBySkuIgnoreCase("FD-COF-IL-001")).thenReturn(Optional.of(latte));

        StockInImportResponse response = service.importFromExcel(fromTemplate(
                new String[] {"fd-cof-il-001-m", "", "3", "12.50", "Supplier ABC"},
                new String[] {"", "", "abc", "", ""}), UUID.randomUUID());

        assertThat(response.totalRows()).isEqualTo(2);
        assertThat(response.created()).isEqualTo(1);
        assertThat(response.received()).singleElement()
                .satisfies(result -> assertThat(result.sku()).isEqualTo("FD-COF-IL-001"));
        assertThat(response.errors()).singleElement().satisfies(error -> {
            assertThat(error.rowNumber()).isEqualTo(3);
            assertThat(error.message()).contains("SKU is required", "Quantity must be", "Unit Cost is required");
        });
        ArgumentCaptor<StockInRequest> request = ArgumentCaptor.forClass(StockInRequest.class);
        verify(inventoryService).stockIn(request.capture(), any());
        assertThat(request.getValue().quantity()).isEqualByComparingTo("3");
        assertThat(request.getValue().note()).isEqualTo("Supplier ABC");
    }

    @Test
    void templateListsProductsAndHasTheDocumentedHeaders() throws Exception {
        when(inventoryRepository.findAll()).thenReturn(List.of(inventory(product("FD-COF-IL-001"))));

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(service.generateTemplate()))) {
            Row header = workbook.getSheet("Stock In").getRow(0);
            for (StockInImportColumn column : StockInImportColumn.values()) {
                assertThat(header.getCell(column.templateIndex()).getStringCellValue()).isEqualTo(column.headerLabel());
            }
            assertThat(workbook.getSheet("Products").getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("FD-COF-IL-001");
        }
    }

    private MockMultipartFile fromTemplate(String[]... rows) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(service.generateTemplate()));
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheet("Stock In");
            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.getRow(i + 1);
                for (int c = 0; c < rows[i].length; c++) {
                    if (c != StockInImportColumn.PRODUCT.templateIndex()) {
                        row.createCell(c).setCellValue(rows[i][c]);
                    }
                }
            }
            workbook.write(out);
            return multipart(out.toByteArray());
        }
    }

    private MockMultipartFile legacyWorkbook(String[]... rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet();
            var header = sheet.createRow(0);
            header.createCell(0).setCellValue("sku");
            header.createCell(1).setCellValue("quantity");
            header.createCell(2).setCellValue("unitCost");
            for (int i = 0; i < rows.length; i++) {
                var row = sheet.createRow(i + 1);
                for (int c = 0; c < rows[i].length; c++) {
                    row.createCell(c).setCellValue(rows[i][c]);
                }
            }
            workbook.write(out);
            return multipart(out.toByteArray());
        }
    }

    private MockMultipartFile multipart(byte[] content) {
        return new MockMultipartFile("file", "stock.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);
    }

    private Product product(String sku) {
        Category category = new Category();
        category.setName("Coffee");
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Iced Latte");
        product.setSku(sku);
        product.setCategory(category);
        product.setStockUnit(StockUnit.PACK);
        product.setSellUnit(SellUnit.CUP);
        return product;
    }

    private Inventory inventory(Product product) {
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantityOnHand(BigDecimal.ONE);
        return inventory;
    }
}
