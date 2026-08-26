package org.group1.coffeeshopapi.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
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
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
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
    private final ProductMapper productMapper;
    private final ProductSizeOptionMapper sizeOptionMapper;
    private final FileStorageService fileStorageService;
    private final ActorLookupService actorLookupService;

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request, UUID actorId) {
        if (productRepository.existsBySkuIgnoreCase(request.sku())) {
            throw new DuplicateResourceException("A product with this SKU already exists");
        }
        Category category = findCategory(request.categoryId());

        Product product = new Product();
        product.setName(request.name());
        product.setDescription(request.description());
        product.setSku(request.sku());
        product.setUnit(request.unit());
        product.setPrice(request.price());
        product.setCategory(category);
        product.setCreatedBy(actorId);
        product.setUpdatedBy(actorId);
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
    public ProductResponse update(UUID id, UpdateProductRequest request, UUID actorId) {
        Product product = findById(id);

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.unit() != null) {
            product.setUnit(request.unit());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.categoryId() != null) {
            product.setCategory(findCategory(request.categoryId()));
        }
        if (request.status() != null) {
            product.setStatus(request.status());
        }
        product.setUpdatedBy(actorId);
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
    public ProductResponse setDiscount(UUID id, SetProductDiscountRequest request, UUID actorId) {
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
        product.setUpdatedBy(actorId);
        product = productRepository.save(product);

        return toResponse(product, findInventory(product.getId()));
    }

    @Override
    @Transactional
    public ProductResponse clearDiscount(UUID id, UUID actorId) {
        Product product = findById(id);
        product.setDiscountType(null);
        product.setDiscountValue(null);
        product.setDiscountStartAt(null);
        product.setDiscountEndAt(null);
        product.setUpdatedBy(actorId);
        product = productRepository.save(product);

        return toResponse(product, findInventory(product.getId()));
    }

    @Override
    @Transactional
    public ProductResponse uploadImage(UUID id, MultipartFile file, UUID actorId) {
        Product product = findById(id);
        String previousImageUrl = product.getImageUrl();

        product.setImageUrl(fileStorageService.uploadImage(file, IMAGE_FOLDER));
        product.setUpdatedBy(actorId);
        product = productRepository.save(product);

        if (previousImageUrl != null) {
            fileStorageService.delete(previousImageUrl);
        }

        return toResponse(product, findInventory(product.getId()));
    }

    @Override
    @Transactional
    public ProductResponse removeImage(UUID id, UUID actorId) {
        Product product = findById(id);
        if (product.getImageUrl() != null) {
            fileStorageService.delete(product.getImageUrl());
            product.setImageUrl(null);
            product.setUpdatedBy(actorId);
            product = productRepository.save(product);
        }
        return toResponse(product, findInventory(product.getId()));
    }

    @Override
    @Transactional
    public ProductImportResponse importFromExcel(MultipartFile file, UUID actorId) {
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

            // Row 0 is the header (name, description, sku, unit, price, category, reorderLevel).
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
                String unit = formatter.formatCellValue(row.getCell(3)).trim();
                String priceText = formatter.formatCellValue(row.getCell(4)).trim();
                String categoryName = formatter.formatCellValue(row.getCell(5)).trim();
                String reorderText = formatter.formatCellValue(row.getCell(6)).trim();

                if (name.isBlank() || sku.isBlank() || unit.isBlank() || categoryName.isBlank()) {
                    errors.add(new ProductImportRowError(excelRowNumber, sku,
                            "name, sku, unit and category are required"));
                    continue;
                }

                BigDecimal price = parseDecimal(priceText);
                if (price == null || price.signum() < 0) {
                    errors.add(new ProductImportRowError(excelRowNumber, sku, "Invalid price: " + priceText));
                    continue;
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
                product.setUnit(unit);
                product.setPrice(price);
                product.setCategory(category);
                product.setCreatedBy(actorId);
                product.setUpdatedBy(actorId);
                product = productRepository.save(product);

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
        for (int cellIndex = 0; cellIndex < 7; cellIndex++) {
            String value = formatter.formatCellValue(row.getCell(cellIndex));
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
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
        return productMapper.toResponse(product, inventory,
                actorLookupService.resolve(product.getCreatedBy()),
                actorLookupService.resolve(product.getUpdatedBy()),
                sizeOptions);
    }

    // Batches the createdBy/updatedBy lookups and size options for a whole page instead of
    // resolving each row individually, so listing N products costs a handful of extra queries
    // instead of up to 3N.
    private Page<ProductResponse> toResponsePage(Page<Product> products) {
        Set<UUID> actorIds = new HashSet<>();
        List<UUID> productIds = new ArrayList<>();
        for (Product product : products) {
            actorIds.add(product.getCreatedBy());
            actorIds.add(product.getUpdatedBy());
            productIds.add(product.getId());
        }
        Map<UUID, ActorSummary> actors = actorLookupService.resolveAll(actorIds);

        Map<UUID, List<ProductSizeOptionResponse>> sizeOptionsByProduct = new HashMap<>();
        for (ProductSizeOption sizeOption : sizeOptionRepository
                .findByProductIdInAndStatusOrderBySortOrderAscNameAsc(productIds, Status.ACTIVE)) {
            sizeOptionsByProduct.computeIfAbsent(sizeOption.getProduct().getId(), id -> new ArrayList<>())
                    .add(sizeOptionMapper.toResponse(sizeOption));
        }

        return products.map(product -> productMapper.toResponse(product, findInventory(product.getId()),
                actors.get(product.getCreatedBy()), actors.get(product.getUpdatedBy()),
                sizeOptionsByProduct.getOrDefault(product.getId(), List.of())));
    }
}
