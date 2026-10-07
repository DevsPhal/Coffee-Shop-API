package org.group1.coffeeshopapi.product.service.impl;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.SkuMode;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.product.dto.response.ProductImportResponse;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.excel.ProductImportColumn;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.product.service.ProductSkuGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImportServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductSkuGenerator skuGenerator;
    @InjectMocks private ProductImportServiceImpl service;

    private final Category coffee = category("Coffee", CategoryGroup.FRESH_DRINK);
    private final Category pastry = category("Pastry", CategoryGroup.SNACK);

    @Test
    void templateHasTheDocumentedHeadersAndReferenceSheets() throws Exception {
        when(categoryRepository.findAll(any(Sort.class))).thenReturn(List.of(coffee, pastry));
        when(skuGenerator.categoryPrefix(any())).thenReturn("FD-COF-");

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(service.generateTemplate()))) {
            Row header = workbook.getSheetAt(0).getRow(0);
            List<ProductImportColumn> columns = ProductImportColumn.templateColumns();
            assertThat(workbook.getSheetName(0)).isEqualTo("Products");
            for (int i = 0; i < columns.size(); i++) {
                assertThat(header.getCell(i).getStringCellValue()).isEqualTo(columns.get(i).headerLabel());
            }
            assertThat(workbook.getSheet("Categories").getRow(1).getCell(0).getStringCellValue()).isEqualTo("Coffee");
            assertThat(workbook.getSheet("Instructions")).isNotNull();
        }
    }

    @Test
    void filledTemplateCreatesDrinkWithBothSizesAndAGeneratedSku() throws Exception {
        stubCategoriesAndSaves();
        when(skuGenerator.generate(eq(coffee), eq("Iced Latte"), anySet())).thenReturn("FD-COF-IL-001");

        ProductImportResponse response = service.importFromExcel(file(
                row("Iced Latte", "Coffee", "", "2.50", "3.00", "PACK", "", "40", "5")), admin());

        assertThat(response.created()).isEqualTo(1);
        assertThat(response.errors()).isEmpty();
        assertThat(response.createdProducts()).singleElement().satisfies(result -> {
            assertThat(result.sku()).isEqualTo("FD-COF-IL-001");
            assertThat(result.skuMode()).isEqualTo(SkuMode.GENERATE);
        });
        ArgumentCaptor<Product> product = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(product.capture());
        assertThat(product.getValue().getSellUnit()).isEqualTo(SellUnit.CUP);
        ArgumentCaptor<ProductVariant> variants = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository, times(2)).save(variants.capture());
        assertThat(variants.getAllValues()).extracting(ProductVariant::getName)
                .containsExactly(VariantLabel.MEDIUM, VariantLabel.LARGE);
    }

    @Test
    void manualSkuIsKeptAndSnackGetsAPieceVariant() throws Exception {
        stubCategoriesAndSaves();

        ProductImportResponse response = service.importFromExcel(file(
                row("Croissant", "pastry", "sn-own-01", "1.80", "", "PIECE", "", "", "")), admin());

        assertThat(response.createdProducts()).singleElement().satisfies(result -> {
            assertThat(result.sku()).isEqualTo("SN-OWN-01");
            assertThat(result.skuMode()).isEqualTo(SkuMode.MANUAL);
        });
        ArgumentCaptor<ProductVariant> variant = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).save(variant.capture());
        assertThat(variant.getValue().getName()).isEqualTo(VariantLabel.PIECE);
        verify(skuGenerator, never()).generate(any(), any(), anySet());
    }

    @Test
    void badRowsAreReportedWithEveryProblemWhileGoodRowsAreCreated() throws Exception {
        stubCategoriesAndSaves();
        when(productRepository.existsByNameIgnoreCaseAndCategoryId("Mocha", coffee.getId())).thenReturn(true);

        ProductImportResponse response = service.importFromExcel(file(
                row("Croissant", "Pastry", "", "1.80", "2.20", "PIECE", "", "", ""),
                row("", "Juice", "", "abc", "", "BAG", "", "", ""),
                row("Mocha", "Coffee", "", "2.00", "", "PACK", "", "", ""),
                row("Muffin", "Pastry", "SN-MUF-1", "1.50", "", "PIECE", "", "", "")), admin());

        assertThat(response.totalRows()).isEqualTo(4);
        assertThat(response.created()).isEqualTo(1);
        assertThat(response.errors()).hasSize(3);
        assertThat(response.errors().get(0).rowNumber()).isEqualTo(2);
        assertThat(response.errors().get(0).message()).contains("Large Price only applies to drink categories");
        assertThat(response.errors().get(1).message())
                .contains("Name is required", "Category 'Juice' does not exist", "Stock Unit 'BAG'", "Price must be");
        assertThat(response.errors().get(2).message()).contains("already exists in category 'Coffee'");
    }

    @Test
    void sameSkuTwiceInAFileIsRejectedOnTheSecondRow() throws Exception {
        stubCategoriesAndSaves();

        ProductImportResponse response = service.importFromExcel(file(
                row("Muffin", "Pastry", "SN-1", "1.50", "", "PIECE", "", "", ""),
                row("Cookie", "Pastry", "sn-1", "1.00", "", "PIECE", "", "", "")), admin());

        assertThat(response.created()).isEqualTo(1);
        assertThat(response.errors()).singleElement()
                .satisfies(error -> assertThat(error.message()).contains("more than once"));
    }

    private void stubCategoriesAndSaves() {
        when(categoryRepository.findAll()).thenReturn(List.of(coffee, pastry));
        lenient().when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(UUID.randomUUID());
            return product;
        });
    }

    private String[] row(String name, String category, String sku, String price, String largePrice,
            String stockUnit, String sellUnit, String unitsPerStock, String reorderLevel) {
        return new String[] {name, "", "", category, sku, price, largePrice, stockUnit, sellUnit, unitsPerStock,
                reorderLevel};
    }

    private MockMultipartFile file(String[]... rows) throws Exception {
        when(categoryRepository.findAll(any(Sort.class))).thenReturn(List.of(coffee, pastry));
        when(skuGenerator.categoryPrefix(any())).thenReturn("XX-");
        byte[] template = service.generateTemplate();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(template));
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheet("Products");
            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 1);
                for (int c = 0; c < rows[i].length; c++) {
                    row.createCell(c).setCellValue(rows[i][c]);
                }
            }
            workbook.write(out);
            return new MockMultipartFile("file", "products.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    private Category category(String name, CategoryGroup group) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(name);
        category.setCategoryGroup(group);
        return category;
    }

    private Admin admin() {
        Admin admin = new Admin();
        admin.setId(UUID.randomUUID());
        return admin;
    }
}
