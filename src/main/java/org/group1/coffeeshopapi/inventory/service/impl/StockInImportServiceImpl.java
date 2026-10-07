package org.group1.coffeeshopapi.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.excel.ExcelSheetReader;
import org.group1.coffeeshopapi.common.excel.ExcelTemplateWriter;
import org.group1.coffeeshopapi.common.exception.ApiException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.inventory.dto.request.StockInRequest;
import org.group1.coffeeshopapi.inventory.dto.response.StockInImportResponse;
import org.group1.coffeeshopapi.inventory.dto.response.StockInImportRowError;
import org.group1.coffeeshopapi.inventory.dto.response.StockInImportRowResult;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.excel.StockInImportColumn;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.inventory.service.StockInImportService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.group1.coffeeshopapi.inventory.excel.StockInImportColumn.*;

@Service
@RequiredArgsConstructor
public class StockInImportServiceImpl implements StockInImportService {

    public static final String DATA_SHEET = "Stock In";
    private static final String PRODUCT_SHEET = "Products";

    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] generateTemplate() {
        List<Inventory> inventories = inventoryRepository.findAll().stream()
                .sorted(Comparator.comparing((Inventory inventory) -> inventory.getProduct().getName(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();

        try (ExcelTemplateWriter writer = new ExcelTemplateWriter()) {
            Sheet sheet = writer.dataSheet(DATA_SHEET, StockInImportColumn.templateColumns());
            writer.textColumn(sheet, SKU.templateIndex());
            if (!inventories.isEmpty()) {
                writer.rangeValidation(sheet, SKU.templateIndex(),
                        PRODUCT_SHEET + "!$A$2:$A$" + (inventories.size() + 1),
                        "Pick a product SKU from the list (see the Products sheet)");
            }
            writer.decimalValidation(sheet, QUANTITY.templateIndex(), "0", false);
            writer.decimalValidation(sheet, UNIT_COST.templateIndex(), "0", true);
            writer.formulaColumn(sheet, PRODUCT.templateIndex(),
                    "IF(A{row}=\"\",\"\",IFERROR(VLOOKUP(A{row}," + PRODUCT_SHEET + "!$A:$E,2,FALSE)&\" (\"&VLOOKUP(A{row},"
                            + PRODUCT_SHEET + "!$A:$E,4,FALSE)&\")\",\"Unknown SKU\"))");

            List<Object[]> productRows = inventories.stream()
                    .map(inventory -> {
                        Product product = inventory.getProduct();
                        return new Object[] {
                                product.getSku(),
                                product.getName(),
                                product.getCategory().getName(),
                                product.getStockUnit().name(),
                                inventory.getQuantityOnHand(),
                                inventory.getReorderLevel(),
                                product.getSellUnit().name(),
                                product.getUnitsPerStock(),
                                product.getStatus().name()};
                    })
                    .toList();
            writer.referenceSheet(PRODUCT_SHEET,
                    new String[] {"SKU", "Product", "Category", "Stock Unit", "On Hand", "Reorder Level",
                            "Sell Unit", "Units Per Stock", "Status"},
                    productRows);

            writer.guideSheet("Stock-in import — how to fill this file", List.of(
                    "Fill one delivery line per row on the 'Stock In' sheet, starting at row 2. Do not rename, move or delete the header row.",
                    "Red headers marked * are required, grey headers are optional, blue headers are filled automatically. Hover over a header to see its help note.",
                    "Pick the SKU from the dropdown. The Product column then shows the product name and its stock unit so you can double-check.",
                    "Quantity and Unit Cost are in the product's STOCK unit (PACK, BOX, CARTON or PIECE), not per cup or plate.",
                    "Each row creates a stock batch (used for FIFO costing), a STOCK_IN movement and a stock expense of Quantity x Unit Cost.",
                    "The same SKU may appear on several rows, e.g. two deliveries at different costs.",
                    "Products not listed on the Products sheet must be created first (one by one or with the product import template).",
                    "Upload the file to the stock-in import. Each valid row is saved on its own; invalid rows are listed with their Excel row number and reason, so fix and re-upload only those rows.",
                    "At most " + ExcelSheetReader.MAX_DATA_ROWS + " rows per file."),
                    StockInImportColumn.templateColumns(),
                    List.of(
                            new String[] {"FD-COF-IL-001", "Iced Latte (PACK)", "3", "12.50", "Supplier ABC, invoice #1024"},
                            new String[] {"SN-PAS-BC-001", "Butter Croissant (PIECE)", "40", "0.60", ""}));
            return writer.toBytes();
        } catch (IOException e) {
            throw new InvalidOperationException("Unable to generate stock-in import template: " + e.getMessage());
        }
    }

    @Override
    public StockInImportResponse importFromExcel(MultipartFile file, UUID performedBy) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException("Excel file is required");
        }

        List<StockInImportRowResult> received = new ArrayList<>();
        List<StockInImportRowError> errors = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        int totalRows = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            ExcelSheetReader<StockInImportColumn> reader =
                    ExcelSheetReader.open(workbook, DATA_SHEET, StockInImportColumn.class);

            for (int rowIndex = 1; rowIndex <= reader.lastRowIndex(); rowIndex++) {
                Row row = reader.row(rowIndex);
                if (reader.isBlank(row)) {
                    continue;
                }
                totalRows++;
                int excelRowNumber = rowIndex + 1;

                String sku = reader.text(row, SKU).toUpperCase(Locale.ROOT);
                String quantityText = reader.text(row, QUANTITY);
                String unitCostText = reader.text(row, UNIT_COST);
                String note = reader.text(row, NOTE);

                List<String> problems = new ArrayList<>();
                Product product = null;
                if (sku.isBlank()) {
                    problems.add("SKU is required");
                } else {
                    product = findProduct(sku).orElse(null);
                    if (product == null) {
                        problems.add("No product has SKU " + sku);
                    }
                }
                BigDecimal quantity = ExcelSheetReader.parseDecimal(quantityText);
                if (quantity == null || quantity.signum() <= 0) {
                    problems.add(quantityText.isBlank() ? "Quantity is required"
                            : "Quantity must be a number greater than 0, got '" + quantityText + "'");
                }
                BigDecimal unitCost = ExcelSheetReader.parseDecimal(unitCostText);
                if (unitCost == null || unitCost.signum() < 0) {
                    problems.add(unitCostText.isBlank() ? "Unit Cost is required"
                            : "Unit Cost must be a number of 0 or more, got '" + unitCostText + "'");
                }
                if (!problems.isEmpty()) {
                    errors.add(new StockInImportRowError(excelRowNumber, sku, String.join("; ", problems)));
                    continue;
                }

                try {
                    inventoryService.stockIn(new StockInRequest(product.getId(), quantity, unitCost,
                            note.isBlank() ? null : note), performedBy);
                    BigDecimal amount = quantity.multiply(unitCost);
                    totalCost = totalCost.add(amount);
                    received.add(new StockInImportRowResult(excelRowNumber, product.getId(), product.getSku(),
                            product.getName(), quantity, product.getStockUnit(), unitCost, amount));
                } catch (ApiException e) {
                    errors.add(new StockInImportRowError(excelRowNumber, sku, e.getMessage()));
                }
            }
        } catch (ApiException e) {
            throw e;
        } catch (IOException e) {
            throw new InvalidOperationException("Unable to read Excel file: " + e.getMessage());
        } catch (Exception e) {
            throw new InvalidOperationException("Invalid Excel file: " + e.getMessage());
        }

        return new StockInImportResponse(totalRows, received.size(), errors.size(), totalCost, received, errors);
    }

    private Optional<Product> findProduct(String sku) {
        Optional<Product> product = productRepository.findBySkuIgnoreCase(sku);
        if (product.isPresent()) {
            return product;
        }
        int lastDash = sku.lastIndexOf('-');
        if (lastDash <= 0) {
            return Optional.empty();
        }
        String suffix = sku.substring(lastDash + 1);
        for (VariantLabel label : VariantLabel.values()) {
            if (label.getSkuSuffix().equals(suffix)) {
                return productRepository.findBySkuIgnoreCase(sku.substring(0, lastDash));
            }
        }
        return Optional.empty();
    }
}
