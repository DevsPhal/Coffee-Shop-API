package org.group1.coffeeshopapi.product.service.impl;

import lombok.RequiredArgsConstructor;
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
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.excel.ExcelSheetReader;
import org.group1.coffeeshopapi.common.excel.ExcelTemplateWriter;
import org.group1.coffeeshopapi.common.exception.ApiException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.product.dto.response.ProductImportResponse;
import org.group1.coffeeshopapi.product.dto.response.ProductImportRowError;
import org.group1.coffeeshopapi.product.dto.response.ProductImportRowResult;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.excel.ProductImportColumn;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.product.service.ProductImportService;
import org.group1.coffeeshopapi.product.service.ProductSkuGenerator;
import org.group1.coffeeshopapi.product.service.ProductVariantPolicy;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.group1.coffeeshopapi.product.excel.ProductImportColumn.*;

@Service
@RequiredArgsConstructor
public class ProductImportServiceImpl implements ProductImportService {

    public static final String DATA_SHEET = "Products";
    private static final String CATEGORY_SHEET = "Categories";
    private static final int MAX_TEXT_LENGTH = 255;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductSkuGenerator skuGenerator;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateTemplate() {
        List<Category> categories = categoryRepository.findAll(Sort.by("name"));
        List<ProductImportColumn> columns = ProductImportColumn.templateColumns();

        try (ExcelTemplateWriter writer = new ExcelTemplateWriter()) {
            Sheet sheet = writer.dataSheet(DATA_SHEET, columns);
            writer.textColumn(sheet, SKU.templateIndex());
            writer.textColumn(sheet, NAME_KH.templateIndex());
            if (!categories.isEmpty()) {
                writer.rangeValidation(sheet, CATEGORY.templateIndex(),
                        CATEGORY_SHEET + "!$A$2:$A$" + (categories.size() + 1),
                        "Pick a category from the list (see the Categories sheet)");
            }
            writer.listValidation(sheet, STOCK_UNIT.templateIndex(), names(StockUnit.values()));
            writer.listValidation(sheet, SELL_UNIT.templateIndex(), names(SellUnit.values()));
            writer.decimalValidation(sheet, PRICE.templateIndex(), "0", true);
            writer.decimalValidation(sheet, LARGE_PRICE.templateIndex(), "0", true);
            writer.decimalValidation(sheet, UNITS_PER_STOCK.templateIndex(), "0", false);
            writer.decimalValidation(sheet, REORDER_LEVEL.templateIndex(), "0", true);

            List<Object[]> categoryRows = categories.stream()
                    .map(category -> new Object[] {
                            category.getName(),
                            category.getCategoryGroup() != null ? category.getCategoryGroup().name() : "",
                            ProductVariantPolicy.allowedVariants(category.getCategoryGroup()).stream()
                                    .map(Enum::name).collect(Collectors.joining(", ")),
                            ProductVariantPolicy.allowsSizeChoice(category.getCategoryGroup())
                                    ? "Price = MEDIUM, Large Price = LARGE" : "Price = per PIECE, no Large Price",
                            defaultSellUnit(category.getCategoryGroup()).name(),
                            skuGenerator.categoryPrefix(category) + "...",
                            category.getStatus().name()})
                    .toList();
            writer.referenceSheet(CATEGORY_SHEET,
                    new String[] {"Category", "Group", "Allowed Variants", "How to fill prices",
                            "Default Sell Unit", "Generated SKU Prefix", "Status"},
                    categoryRows);

            writer.guideSheet("Product import — how to fill this file", List.of(
                    "Fill one product per row on the 'Products' sheet, starting at row 2. Do not rename, move or delete the header row.",
                    "Red headers marked * are required, grey headers are optional. Hover over a header to see its help note.",
                    "Category must match an existing category exactly — use the dropdown. Create missing categories in the admin panel first, then download this template again.",
                    "SKU: leave it blank to let the system generate one from the category and name (e.g. FD-COF-IL-001), or type your own to use it as-is. Every SKU must be unique.",
                    "Drink categories (FRESH_DRINK, BEVERAGE): Price is the MEDIUM size and Large Price is the optional LARGE size. Snack and other categories: Price is per PIECE and Large Price must stay blank.",
                    "A product with the same name in the same category is rejected, so re-uploading a file never creates duplicates.",
                    "New products start with 0 stock. Use the stock-in import template with the SKUs returned by this import to receive stock.",
                    "Upload the file to the product import. Valid rows are created; invalid rows are listed with their Excel row number and the reason, so you can fix only those rows and upload them again.",
                    "At most " + ExcelSheetReader.MAX_DATA_ROWS + " rows per file."),
                    columns,
                    List.of(
                            new String[] {"Iced Latte", "ឡាតេទឹកកក", "Espresso with cold milk over ice", "Coffee", "",
                                    "2.50", "3.00", "PACK", "CUP", "40", "5"},
                            new String[] {"Butter Croissant", "", "Baked fresh every morning", "Pastry", "SN-PAS-BC-001",
                                    "1.80", "", "PIECE", "PIECE", "1", "10"},
                            new String[] {"Mineral Water", "", "500 ml bottle", "Soft Drinks", "",
                                    "0.75", "", "CARTON", "BOTTLE", "24", "2"}));
            return writer.toBytes();
        } catch (IOException e) {
            throw new InvalidOperationException("Unable to generate product import template: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ProductImportResponse importFromExcel(MultipartFile file, Admin actorAdmin) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException("Excel file is required");
        }

        Map<String, Category> categoriesByName = new HashMap<>();
        for (Category category : categoryRepository.findAll()) {
            categoriesByName.put(category.getName().toLowerCase(Locale.ROOT), category);
        }

        List<ProductImportRowResult> createdProducts = new ArrayList<>();
        List<ProductImportRowError> errors = new ArrayList<>();
        Set<String> skusInFile = new HashSet<>();
        Set<String> namesInFile = new HashSet<>();
        int totalRows = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            ExcelSheetReader<ProductImportColumn> reader =
                    ExcelSheetReader.open(workbook, DATA_SHEET, ProductImportColumn.class);

            for (int rowIndex = 1; rowIndex <= reader.lastRowIndex(); rowIndex++) {
                Row row = reader.row(rowIndex);
                if (reader.isBlank(row)) {
                    continue;
                }
                totalRows++;
                int excelRowNumber = rowIndex + 1;
                String rawSku = reader.text(row, SKU);

                ParsedProduct parsed;
                try {
                    parsed = parseRow(reader, row, categoriesByName);
                } catch (RowException e) {
                    errors.add(new ProductImportRowError(excelRowNumber, rawSku, e.getMessage()));
                    continue;
                }

                String nameKey = parsed.category().getId() + "|" + parsed.name().toLowerCase(Locale.ROOT);
                if (!namesInFile.add(nameKey)) {
                    errors.add(new ProductImportRowError(excelRowNumber, rawSku, "'" + parsed.name()
                            + "' appears more than once in category '" + parsed.category().getName() + "' in this file"));
                    continue;
                }
                if (productRepository.existsByNameIgnoreCaseAndCategoryId(parsed.name(), parsed.category().getId())) {
                    errors.add(new ProductImportRowError(excelRowNumber, rawSku, "A product named '" + parsed.name()
                            + "' already exists in category '" + parsed.category().getName() + "'"));
                    continue;
                }

                SkuMode skuMode = rawSku.isBlank() ? SkuMode.GENERATE : SkuMode.MANUAL;
                String sku;
                try {
                    sku = skuMode == SkuMode.GENERATE
                            ? skuGenerator.generate(parsed.category(), parsed.name(), skusInFile)
                            : ProductSkuGenerator.normalizeManual(rawSku);
                } catch (ApiException e) {
                    errors.add(new ProductImportRowError(excelRowNumber, rawSku, e.getMessage()));
                    continue;
                }
                if (skusInFile.contains(sku)) {
                    errors.add(new ProductImportRowError(excelRowNumber, sku, "SKU " + sku + " is used more than once in this file"));
                    continue;
                }
                if (productRepository.existsBySkuIgnoreCase(sku)) {
                    errors.add(new ProductImportRowError(excelRowNumber, sku, "SKU " + sku + " already belongs to another product"));
                    continue;
                }
                skusInFile.add(sku);

                Product product = save(parsed, sku, actorAdmin);
                createdProducts.add(new ProductImportRowResult(excelRowNumber, product.getId(), product.getName(),
                        sku, skuMode));
            }
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidOperationException("Unable to read Excel file: " + e.getMessage());
        } catch (Exception e) {
            throw new InvalidOperationException("Invalid Excel file: " + e.getMessage());
        }

        return new ProductImportResponse(totalRows, createdProducts.size(), errors.size(), createdProducts, errors);
    }

    private ParsedProduct parseRow(ExcelSheetReader<ProductImportColumn> reader, Row row,
            Map<String, Category> categoriesByName) {
        List<String> problems = new ArrayList<>();

        String name = reader.text(row, NAME);
        String nameKh = reader.text(row, NAME_KH);
        String description = reader.text(row, DESCRIPTION);
        String categoryName = reader.text(row, CATEGORY);
        String stockUnitText = reader.text(row, STOCK_UNIT);
        String sellUnitText = reader.text(row, SELL_UNIT);
        String priceText = reader.text(row, PRICE);
        String largePriceText = reader.text(row, LARGE_PRICE);
        String variantsText = reader.text(row, VARIANTS);
        String unitsPerStockText = reader.text(row, UNITS_PER_STOCK);
        String reorderText = reader.text(row, REORDER_LEVEL);

        if (name.isBlank()) {
            problems.add("Name is required");
        }
        checkLength(problems, NAME, name);
        checkLength(problems, NAME_KH, nameKh);
        checkLength(problems, DESCRIPTION, description);

        Category category = null;
        if (categoryName.isBlank()) {
            problems.add("Category is required");
        } else {
            category = categoriesByName.get(categoryName.toLowerCase(Locale.ROOT));
            if (category == null) {
                problems.add("Category '" + categoryName + "' does not exist — create it first or pick one from the list");
            }
        }
        CategoryGroup group = category != null ? category.getCategoryGroup() : null;

        StockUnit stockUnit = null;
        if (stockUnitText.isBlank()) {
            problems.add("Stock Unit is required");
        } else {
            stockUnit = ExcelSheetReader.parseEnum(StockUnit.class, stockUnitText);
            if (stockUnit == null) {
                problems.add("Stock Unit '" + stockUnitText + "' is not one of " + Arrays.toString(StockUnit.values()));
            }
        }

        SellUnit sellUnit = defaultSellUnit(group);
        if (!sellUnitText.isBlank()) {
            sellUnit = ExcelSheetReader.parseEnum(SellUnit.class, sellUnitText);
            if (sellUnit == null) {
                problems.add("Sell Unit '" + sellUnitText + "' is not one of " + Arrays.toString(SellUnit.values()));
            }
        }

        BigDecimal unitsPerStock = BigDecimal.ONE;
        if (!unitsPerStockText.isBlank()) {
            unitsPerStock = ExcelSheetReader.parseDecimal(unitsPerStockText);
            if (unitsPerStock == null || unitsPerStock.signum() <= 0) {
                problems.add("Units Per Stock must be a number greater than 0, got '" + unitsPerStockText + "'");
            }
        }

        BigDecimal reorderLevel = BigDecimal.ZERO;
        if (!reorderText.isBlank()) {
            reorderLevel = ExcelSheetReader.parseDecimal(reorderText);
            if (reorderLevel == null || reorderLevel.signum() < 0) {
                problems.add("Reorder Level must be a number of 0 or more, got '" + reorderText + "'");
            }
        }

        Map<VariantLabel, BigDecimal> variants = new LinkedHashMap<>();
        if (!variantsText.isBlank()) {
            parseVariants(variantsText, variants, problems);
        } else {
            BigDecimal price = ExcelSheetReader.parseDecimal(priceText);
            if (priceText.isBlank()) {
                problems.add("Price is required");
            } else if (price == null || price.signum() < 0) {
                problems.add("Price must be a number of 0 or more, got '" + priceText + "'");
            } else {
                variants.put(ProductVariantPolicy.defaultVariant(group), price);
            }
            if (!largePriceText.isBlank()) {
                BigDecimal largePrice = ExcelSheetReader.parseDecimal(largePriceText);
                if (largePrice == null || largePrice.signum() < 0) {
                    problems.add("Large Price must be a number of 0 or more, got '" + largePriceText + "'");
                } else {
                    variants.put(VariantLabel.LARGE, largePrice);
                }
            }
        }
        if (category != null) {
            Set<VariantLabel> allowed = ProductVariantPolicy.allowedVariants(group);
            for (VariantLabel label : variants.keySet()) {
                if (!allowed.contains(label)) {
                    problems.add(label == VariantLabel.LARGE && variants.containsKey(VariantLabel.PIECE)
                            ? "Large Price only applies to drink categories — '" + category.getName() + "' is "
                                    + ProductVariantPolicy.describe(group) + ", leave it blank"
                            : "Variant " + label + " is not allowed for '" + category.getName() + "' ("
                                    + ProductVariantPolicy.describe(group) + "); allowed: " + allowed);
                }
            }
        }

        if (!problems.isEmpty()) {
            throw new RowException(String.join("; ", problems));
        }
        return new ParsedProduct(name, blankToNull(nameKh), blankToNull(description), category, stockUnit, sellUnit,
                unitsPerStock, reorderLevel, variants);
    }

    private void parseVariants(String text, Map<VariantLabel, BigDecimal> variants, List<String> problems) {
        for (String pair : text.split(";")) {
            if (pair.isBlank()) {
                continue;
            }
            String[] parts = pair.split(":", 2);
            if (parts.length != 2) {
                problems.add("Variants must look like MEDIUM:2.50;LARGE:3.00, got '" + pair.trim() + "'");
                continue;
            }
            VariantLabel label = ExcelSheetReader.parseEnum(VariantLabel.class, parts[0]);
            BigDecimal price = ExcelSheetReader.parseDecimal(parts[1]);
            if (label == null) {
                problems.add("Variant '" + parts[0].trim() + "' is not one of " + Arrays.toString(VariantLabel.values()));
            } else if (price == null || price.signum() < 0) {
                problems.add("Variant " + label + " has an invalid price '" + parts[1].trim() + "'");
            } else if (variants.putIfAbsent(label, price) != null) {
                problems.add("Variant " + label + " is listed twice");
            }
        }
        if (variants.isEmpty() && problems.isEmpty()) {
            problems.add("Variants column is blank");
        }
    }

    private Product save(ParsedProduct parsed, String sku, Admin actorAdmin) {
        Product product = new Product();
        product.setName(parsed.name());
        product.setNameKh(parsed.nameKh());
        product.setDescription(parsed.description());
        product.setSku(sku);
        product.setStockUnit(parsed.stockUnit());
        product.setSellUnit(parsed.sellUnit());
        product.setUnitsPerStock(parsed.unitsPerStock());
        product.setCategory(parsed.category());
        product.setCreatedByAdmin(actorAdmin);
        product.setUpdatedByAdmin(actorAdmin);
        product = productRepository.save(product);

        int sortOrder = 1;
        for (Map.Entry<VariantLabel, BigDecimal> entry : parsed.variants().entrySet()) {
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setName(entry.getKey());
            variant.setPrice(entry.getValue());
            variant.setSortOrder(sortOrder++);
            variantRepository.save(variant);
        }

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantityOnHand(BigDecimal.ZERO);
        inventory.setReorderLevel(parsed.reorderLevel());
        inventoryRepository.save(inventory);
        return product;
    }

    private static SellUnit defaultSellUnit(CategoryGroup group) {
        return ProductVariantPolicy.allowsSizeChoice(group) ? SellUnit.CUP : SellUnit.PIECE;
    }

    private static void checkLength(List<String> problems, ProductImportColumn column, String value) {
        if (value.length() > MAX_TEXT_LENGTH) {
            problems.add(column.header() + " must not exceed " + MAX_TEXT_LENGTH + " characters");
        }
    }

    private static String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }

    private static String[] names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toArray(String[]::new);
    }

    private record ParsedProduct(String name, String nameKh, String description, Category category,
            StockUnit stockUnit, SellUnit sellUnit, BigDecimal unitsPerStock, BigDecimal reorderLevel,
            Map<VariantLabel, BigDecimal> variants) {
    }

    private static final class RowException extends RuntimeException {
        private RowException(String message) {
            super(message);
        }
    }
}
