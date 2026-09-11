package org.group1.coffeeshopapi.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.extra.dto.response.ProductExtraResponse;
import org.group1.coffeeshopapi.extra.entity.ProductExtra;
import org.group1.coffeeshopapi.extra.mapper.ProductExtraMapper;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.group1.coffeeshopapi.product.dto.request.CreateProductRequest;
import org.group1.coffeeshopapi.product.dto.request.SetProductDiscountRequest;
import org.group1.coffeeshopapi.product.dto.request.UpdateProductRequest;
import org.group1.coffeeshopapi.product.dto.response.ProductImportResponse;
import org.group1.coffeeshopapi.product.dto.response.ProductImportRowError;
import org.group1.coffeeshopapi.product.dto.response.ProductResponse;
import org.group1.coffeeshopapi.product.dto.response.ProductSizeOptionResponse;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductSizeOption;
import org.group1.coffeeshopapi.product.mapper.ProductMapper;
import org.group1.coffeeshopapi.product.mapper.ProductSizeOptionMapper;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductSizeOptionRepository;
import org.group1.coffeeshopapi.product.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String IMAGE_FOLDER = "products";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductSizeOptionRepository sizeOptionRepository;
    private final ProductExtraRepository productExtraRepository;
    private final ProductMapper productMapper;
    private final ProductSizeOptionMapper sizeOptionMapper;
    private final ProductExtraMapper productExtraMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request, Admin actorAdmin) {
        if (productRepository.existsBySkuIgnoreCase(request.sku())) {
            throw new DuplicateResourceException("A product with this SKU already exists");
        }
        Category category = findCategory(request.categoryId());

        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setSku(request.sku());
        product.setStockUnit(request.stockUnit());
        product.setSellUnit(request.sellUnit());
        product.setUnitsPerStock(request.unitsPerStock() != null ? request.unitsPerStock() : BigDecimal.ONE);
        product.setCategory(category);
        product.setCreatedByAdmin(actorAdmin);
        product.setUpdatedByAdmin(actorAdmin);
        product = productRepository.save(product);

        // Every product gets exactly one inventory record the moment it's created, so stock-in/
        // stock-cut never has to special-case a product with no inventory row yet.
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantityOnHand(BigDecimal.ZERO);
        inventory.setReorderLevel(request.reorderLevel() != null ? request.reorderLevel() : BigDecimal.ZERO);
        inventoryRepository.save(inventory);

        return toResponse(product, inventory);
    }

    @Override
    public ProductResponse getById(UUID id) {
        Product product = findById(id);
        return toResponse(product, findInventory(product.getId()));
    }

    @Override
    public Page<ProductResponse> list(UUID categoryId, Pageable pageable) {
        Page<Product> products = categoryId != null
                ? productRepository.findByCategoryId(categoryId, pageable)
                : productRepository.findAll(pageable);
        return toResponsePage(products);
    }

    @Override
    public Page<ProductResponse> listActive(UUID categoryId, Pageable pageable) {
        Page<Product> products = categoryId != null
                ? productRepository.findByCategoryIdAndStatus(categoryId, Status.ACTIVE, pageable)
                : productRepository.findByStatus(Status.ACTIVE, pageable);
        return toResponsePage(products);
    }

    @Override
    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request, Admin actorAdmin) {
        Product product = findById(id);

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.stockUnit() != null) {
            product.setStockUnit(request.stockUnit());
        }
        if (request.sellUnit() != null) {
            product.setSellUnit(request.sellUnit());
        }
        if (request.unitsPerStock() != null) {
            product.setUnitsPerStock(request.unitsPerStock());
        }
        if (request.categoryId() != null) {
            product.setCategory(findCategory(request.categoryId()));
        }
        if (request.status() != null) {
            product.setStatus(request.status());
        }
        product.setUpdatedByAdmin(actorAdmin);
        product = productRepository.save(product);

        Inventory inventory = findInventory(product.getId());
        if (request.reorderLevel() != null) {
            inventory.setReorderLevel(request.reorderLevel());
            inventoryRepository.save(inventory);
        }

        return toResponse(product, inventory);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Product product = findById(id);
        Inventory inventory = findInventory(product.getId());
        if (inventory.getQuantityOnHand().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidOperationException("Cannot delete a product that still has stock on hand");
        }
        inventoryRepository.delete(inventory);
        productRepository.delete(product);
    }

    @Override
    @Transactional
    public ProductResponse setDiscount(UUID id, SetProductDiscountRequest request, Admin actorAdmin) {
        Product product = findById(id);

        if (request.discountType() == DiscountType.PERCENTAGE
                && request.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new InvalidOperationException("Percentage discount cannot exceed 100");
        }
        if (request.discountStartAt() != null && request.discountEndAt() != null
                && !request.discountEndAt().isAfter(request.discountStartAt())) {
            throw new InvalidOperationException("Discount end date must be after the start date");
        }

        product.setDiscountType(request.discountType());
        product.setDiscountValue(request.discountValue());
        product.setDiscountStartAt(request.discountStartAt());
        product.setDiscountEndAt(request.discountEndAt());
        product.setUpdatedByAdmin(actorAdmin);
        product = productRepository.save(product);

        return toResponse(product, findInventory(product.getId()));
    }

    @Override
    @Transactional
    public ProductResponse clearDiscount(UUID id, Admin actorAdmin) {
        Product product = findById(id);
        product.setDiscountType(null);
        product.setDiscountValue(null);
        product.setDiscountStartAt(null);
        product.setDiscountEndAt(null);
        product.setUpdatedByAdmin(actorAdmin);
        product = productRepository.save(product);

        return toResponse(product, findInventory(product.getId()));
    }

    @Override
    @Transactional
    public ProductResponse uploadImage(UUID id, MultipartFile file, Admin actorAdmin) {
        Product product = findById(id);
        String previousImageUrl = product.getImageUrl();

        product.setImageUrl(fileStorageService.uploadImage(file, IMAGE_FOLDER));
        product.setUpdatedByAdmin(actorAdmin);
        product = productRepository.save(product);

        if (previousImageUrl != null) {
            fileStorageService.delete(previousImageUrl);
        }

        return toResponse(product, findInventory(product.getId()));
    }

    @Override
    @Transactional
    public ProductResponse removeImage(UUID id, Admin actorAdmin) {
        Product product = findById(id);
        if (product.getImageUrl() != null) {
            fileStorageService.delete(product.getImageUrl());
            product.setImageUrl(null);
            product.setUpdatedByAdmin(actorAdmin);
            product = productRepository.save(product);
        }
        return toResponse(product, findInventory(product.getId()));
    }

    @Override
    @Transactional
    public ProductImportResponse importFromExcel(MultipartFile file, Admin actorAdmin) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException("Excel file is required");
        }

        List<ProductImportRowError> errors = new ArrayList<>();
        Set<String> skusInFile = new HashSet<>();
        int totalRows = 0;
        int created = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            // Row 0 is the header (name, description, sku, stockUnit, price, category,
            // reorderLevel, sizeOptions, sellUnit, unitsPerStock). stockUnit must be one of
            // StockUnit's names (PACK/BOX/CARTON/PIECE).
            //
            // sizeOptions (optional) lets one row seed more than the single default MEDIUM size
            // (see parseSizeOptions): "SMALL:1.25;MEDIUM:1.50;LARGE:1.75". When given, it's the
            // complete set of size options for the row — price is then optional/ignored, since
            // each pair already carries its own price. When left blank, price is required and
            // seeds a single "MEDIUM" size option, same as before this column existed.
            //
            // sellUnit (optional) must be one of SellUnit's names (PLATE/BOTTLE/CAN/CUP/CARTON/
            // PACKAGE/TANK/PIECE) — defaults to CUP when blank, same default this column used to
            // be hardcoded to (see db/add-product-sell-unit-columns.sql for the same default used
            // when this column set was backfilled onto pre-existing rows). unitsPerStock
            // (optional) is how many sellUnits one stockUnit yields, e.g. a CARTON of 24 CANs ->
            // 24 — defaults to 1 when blank.
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isRowEmpty(row, formatter)) {
                    continue;
                }
                totalRows++;
                int excelRowNumber = rowIndex + 1;

                String name = formatter.formatCellValue(row.getCell(0)).trim();
                String description = formatter.formatCellValue(row.getCell(1)).trim();
                String sku = formatter.formatCellValue(row.getCell(2)).trim();
                String unitText = formatter.formatCellValue(row.getCell(3)).trim();
                String priceText = formatter.formatCellValue(row.getCell(4)).trim();
                String categoryName = formatter.formatCellValue(row.getCell(5)).trim();
                String reorderText = formatter.formatCellValue(row.getCell(6)).trim();
                String sizeOptionsText = formatter.formatCellValue(row.getCell(7)).trim();
                String sellUnitText = formatter.formatCellValue(row.getCell(8)).trim();
                String unitsPerStockText = formatter.formatCellValue(row.getCell(9)).trim();

                if (name.isBlank() || sku.isBlank() || unitText.isBlank() || categoryName.isBlank()) {
                    errors.add(new ProductImportRowError(excelRowNumber, sku,
                            "name, sku, unit and category are required"));
                    continue;
                }

                StockUnit stockUnit;
                try {
                    stockUnit = StockUnit.valueOf(unitText.toUpperCase());
                } catch (IllegalArgumentException e) {
                    errors.add(new ProductImportRowError(excelRowNumber, sku, "Invalid unit: " + unitText));
                    continue;
                }

                SellUnit sellUnit = SellUnit.CUP;
                if (!sellUnitText.isBlank()) {
                    try {
                        sellUnit = SellUnit.valueOf(sellUnitText.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        errors.add(new ProductImportRowError(excelRowNumber, sku, "Invalid sell unit: " + sellUnitText));
                        continue;
                    }
                }

                BigDecimal unitsPerStock = BigDecimal.ONE;
                if (!unitsPerStockText.isBlank()) {
                    unitsPerStock = parseDecimal(unitsPerStockText);
                    if (unitsPerStock == null || unitsPerStock.signum() <= 0) {
                        errors.add(new ProductImportRowError(excelRowNumber, sku,
                                "Invalid units per stock: " + unitsPerStockText));
                        continue;
                    }
                }

                List<ParsedSizeOption> sizeOptions;
                if (!sizeOptionsText.isBlank()) {
                    try {
                        sizeOptions = parseSizeOptions(sizeOptionsText);
                    } catch (IllegalArgumentException e) {
                        errors.add(new ProductImportRowError(excelRowNumber, sku, e.getMessage()));
                        continue;
                    }
                } else {
                    BigDecimal price = parseDecimal(priceText);
                    if (price == null || price.signum() < 0) {
                        errors.add(new ProductImportRowError(excelRowNumber, sku, "Invalid price: " + priceText));
                        continue;
                    }
                    sizeOptions = List.of(new ParsedSizeOption("MEDIUM", price));
                }

                BigDecimal reorderLevel = BigDecimal.ZERO;
                if (!reorderText.isBlank()) {
                    reorderLevel = parseDecimal(reorderText);
                    if (reorderLevel == null || reorderLevel.signum() < 0) {
                        errors.add(new ProductImportRowError(excelRowNumber, sku, "Invalid reorder level: " + reorderText));
                        continue;
                    }
                }

                if (!skusInFile.add(sku.toUpperCase())) {
                    errors.add(new ProductImportRowError(excelRowNumber, sku, "Duplicate SKU within the file"));
                    continue;
                }
                if (productRepository.existsBySkuIgnoreCase(sku)) {
                    errors.add(new ProductImportRowError(excelRowNumber, sku, "SKU already exists"));
                    continue;
                }

                Category category = categoryRepository.findByNameIgnoreCase(categoryName).orElse(null);
                if (category == null) {
                    errors.add(new ProductImportRowError(excelRowNumber, sku, "Category not found: " + categoryName));
                    continue;
                }

                // Every field above is validated before this point, so this insert cannot fail —
                // important, because a Postgres constraint violation would abort the whole
                // transaction and silently fail every row after it.
                Product product = new Product();
                product.setName(name);
                product.setDescription(description.isBlank() ? null : description);
                product.setSku(sku);
                product.setStockUnit(stockUnit);
                product.setSellUnit(sellUnit);
                product.setUnitsPerStock(unitsPerStock);
                product.setCategory(category);
                product.setCreatedByAdmin(actorAdmin);
                product.setUpdatedByAdmin(actorAdmin);
                product = productRepository.save(product);

                for (int i = 0; i < sizeOptions.size(); i++) {
                    ParsedSizeOption parsed = sizeOptions.get(i);
                    ProductSizeOption sizeOption = new ProductSizeOption();
                    sizeOption.setProduct(product);
                    sizeOption.setName(parsed.name());
                    sizeOption.setPrice(parsed.price());
                    sizeOption.setSortOrder(i + 1);
                    sizeOptionRepository.save(sizeOption);
                }

                Inventory inventory = new Inventory();
                inventory.setProduct(product);
                inventory.setQuantityOnHand(BigDecimal.ZERO);
                inventory.setReorderLevel(reorderLevel);
                inventoryRepository.save(inventory);

                created++;
            }
        } catch (IOException e) {
            throw new InvalidOperationException("Unable to read Excel file: " + e.getMessage());
        } catch (Exception e) {
            throw new InvalidOperationException("Invalid Excel file: " + e.getMessage());
        }

        return new ProductImportResponse(totalRows, created, errors.size(), errors);
    }

    private boolean isRowEmpty(Row row, DataFormatter formatter) {
        for (int cellIndex = 0; cellIndex < 10; cellIndex++) {
            String value = formatter.formatCellValue(row.getCell(cellIndex));
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private record ParsedSizeOption(String name, BigDecimal price) {
    }

    // "SMALL:1.25;MEDIUM:1.50;LARGE:1.75" -> one ProductSizeOption per "name:price" pair, in the
    // order given (that order becomes each option's sortOrder — see the caller). Throws
    // IllegalArgumentException with a row-error-ready message on anything malformed, so the
    // caller can just surface it as a ProductImportRowError.
    private List<ParsedSizeOption> parseSizeOptions(String text) {
        List<ParsedSizeOption> parsed = new ArrayList<>();
        Set<String> namesSeen = new HashSet<>();
        for (String pair : text.split(";")) {
            if (pair.isBlank()) {
                continue;
            }
            String[] parts = pair.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid size options — expected \"name:price;name:price\", got: " + pair.trim());
            }
            String optionName = parts[0].trim();
            BigDecimal optionPrice = parseDecimal(parts[1].trim());
            if (optionName.isBlank() || optionPrice == null || optionPrice.signum() < 0) {
                throw new IllegalArgumentException("Invalid size option: " + pair.trim());
            }
            if (!namesSeen.add(optionName.toUpperCase())) {
                throw new IllegalArgumentException("Duplicate size option name: " + optionName);
            }
            parsed.add(new ParsedSizeOption(optionName, optionPrice));
        }
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("Size options column is blank");
        }
        return parsed;
    }

    private BigDecimal parseDecimal(String text) {
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private Category findCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private Inventory findInventory(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product"));
    }

    private ProductResponse toResponse(Product product, Inventory inventory) {
        List<ProductSizeOptionResponse> sizeOptions = sizeOptionRepository
                .findByProductIdOrderBySortOrderAscNameAsc(product.getId()).stream()
                .map(sizeOptionMapper::toResponse)
                .toList();
        List<ProductExtraResponse> extras = productExtraRepository
                .findByProductIdOrderBySortOrderAscId(product.getId()).stream()
                .map(productExtraMapper::toResponse)
                .toList();
        return productMapper.toResponse(product, inventory, sizeOptions, extras);
    }

    // Batches size options/extras for a whole page instead of resolving each row individually.
    // createdByAdmin/updatedByAdmin are batched too, but by Hibernate itself — see Admin's
    // @BatchSize — rather than anything explicit here.
    private Page<ProductResponse> toResponsePage(Page<Product> products) {
        List<UUID> productIds = products.stream().map(Product::getId).toList();

        Map<UUID, List<ProductSizeOptionResponse>> sizeOptionsByProduct = new HashMap<>();
        for (ProductSizeOption sizeOption : sizeOptionRepository
                .findByProductIdInAndStatusOrderBySortOrderAscNameAsc(productIds, Status.ACTIVE)) {
            sizeOptionsByProduct.computeIfAbsent(sizeOption.getProduct().getId(), id -> new ArrayList<>())
                    .add(sizeOptionMapper.toResponse(sizeOption));
        }

        Map<UUID, List<ProductExtraResponse>> extrasByProduct = new HashMap<>();
        for (ProductExtra productExtra : productExtraRepository
                .findByProductIdInAndStatusOrderBySortOrderAscId(productIds, Status.ACTIVE)) {
            extrasByProduct.computeIfAbsent(productExtra.getProduct().getId(), id -> new ArrayList<>())
                    .add(productExtraMapper.toResponse(productExtra));
        }

        return products.map(product -> productMapper.toResponse(product, findInventory(product.getId()),
                sizeOptionsByProduct.getOrDefault(product.getId(), List.of()),
                extrasByProduct.getOrDefault(product.getId(), List.of())));
    }
}
