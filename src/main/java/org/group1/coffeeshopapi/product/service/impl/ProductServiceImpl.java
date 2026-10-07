package org.group1.coffeeshopapi.product.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.SkuMode;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.extra.dto.response.ProductExtraResponse;
import org.group1.coffeeshopapi.extra.entity.ProductExtra;
import org.group1.coffeeshopapi.extra.mapper.ProductExtraMapper;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.product.dto.request.CreateProductRequest;
import org.group1.coffeeshopapi.product.dto.request.SetProductDiscountRequest;
import org.group1.coffeeshopapi.product.dto.request.UpdateProductRequest;
import org.group1.coffeeshopapi.product.dto.response.ProductResponse;
import org.group1.coffeeshopapi.product.dto.response.ProductVariantResponse;
import org.group1.coffeeshopapi.product.dto.response.SkuSuggestionResponse;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.mapper.ProductMapper;
import org.group1.coffeeshopapi.product.mapper.ProductVariantMapper;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.product.service.ProductService;
import org.group1.coffeeshopapi.product.service.ProductSkuGenerator;
import org.group1.coffeeshopapi.product.service.ProductVariantPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
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
    private final ProductVariantRepository variantRepository;
    private final ProductExtraRepository productExtraRepository;
    private final ProductMapper productMapper;
    private final ProductVariantMapper variantMapper;
    private final ProductExtraMapper productExtraMapper;
    private final FileStorageService fileStorageService;
    private final ProductSkuGenerator skuGenerator;

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request, Admin actorAdmin) {
        Category category = findCategory(request.categoryId());
        String sku = resolveSkuMode(request.skuMode(), request.sku()) == SkuMode.GENERATE
                ? skuGenerator.generate(category, request.name())
                : ProductSkuGenerator.normalizeManual(request.sku());
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateResourceException("A product with this SKU already exists");
        }

        Product product = new Product();
        product.setName(request.name());
        product.setNameKh(request.nameKh());
        product.setDescription(request.description());
        product.setSku(sku);
        product.setStockUnit(request.stockUnit());
        product.setSellUnit(request.sellUnit());
        product.setUnitsPerStock(request.unitsPerStock() != null ? request.unitsPerStock() : BigDecimal.ONE);
        product.setCategory(category);
        product.setCreatedByAdmin(actorAdmin);
        product.setUpdatedByAdmin(actorAdmin);
        product = productRepository.save(product);

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
    public ProductResponse getOrderableById(UUID id) {
        Product product = findById(id);
        Inventory inventory = findInventory(product.getId());
        if (!product.isAvailableForSale() || inventory.getQuantityOnHand().signum() <= 0) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        return toResponsePage(new PageImpl<>(List.of(product)), true).getContent().get(0);
    }

    @Override
    public Page<ProductResponse> list(UUID categoryId, Pageable pageable) {
        Page<Product> products = categoryId != null
                ? productRepository.findByCategoryId(categoryId, pageable)
                : productRepository.findAll(pageable);
        return toResponsePage(products, false);
    }

    @Override
    public Page<ProductResponse> listActive(UUID categoryId, Pageable pageable) {
        Page<Product> products = categoryId != null
                ? productRepository.findByCategoryIdAndStatusAndInStock(categoryId, Status.ACTIVE, pageable)
                : productRepository.findByStatusAndInStock(Status.ACTIVE, pageable);
        return toResponsePage(products, true);
    }

    @Override
    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request, Admin actorAdmin) {
        Product product = findById(id);

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.nameKh() != null) {
            product.setNameKh(request.nameKh());
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
        if (request.categoryId() != null
                && (product.getCategory() == null || !request.categoryId().equals(product.getCategory().getId()))) {
            Category category = findCategory(request.categoryId());
            ProductVariantPolicy.requireAllAllowed(category.getCategoryGroup(),
                    variantRepository.findByProductIdOrderBySortOrderAscNameAsc(product.getId()).stream()
                            .map(ProductVariant::getName).toList(),
                    product.getName());
            product.setCategory(category);
        }
        if (request.status() != null) {
            product.setStatus(request.status());
        }
        if (request.skuMode() == SkuMode.GENERATE) {
            product.setSku(skuGenerator.regenerate(product));
        } else if (request.sku() != null && !request.sku().isBlank()) {
            String sku = ProductSkuGenerator.normalizeManual(request.sku());
            if (productRepository.existsBySkuIgnoreCaseAndIdNot(sku, product.getId())) {
                throw new DuplicateResourceException("A product with this SKU already exists");
            }
            product.setSku(sku);
        } else if (request.skuMode() == SkuMode.MANUAL) {
            throw new InvalidOperationException("SKU is required when SKU mode is MANUAL");
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
    public ProductResponse regenerateSku(UUID id, Admin actorAdmin) {
        Product product = findById(id);
        product.setSku(skuGenerator.regenerate(product));
        product.setUpdatedByAdmin(actorAdmin);
        product = productRepository.save(product);
        return toResponse(product, findInventory(product.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public SkuSuggestionResponse suggestSku(UUID categoryId, String name, UUID productId) {
        if (name == null || name.isBlank()) {
            throw new InvalidOperationException("Product name is required to generate a SKU");
        }
        Category category = findCategory(categoryId);
        String sku;
        if (productId != null) {
            Product product = findById(productId);
            Product preview = new Product();
            preview.setName(name.trim());
            preview.setCategory(category);
            preview.setSku(product.getSku());
            sku = skuGenerator.regenerate(preview);
        } else {
            sku = skuGenerator.generate(category, name.trim());
        }
        Set<VariantLabel> allowed = ProductVariantPolicy.allowedVariants(category.getCategoryGroup());
        Map<VariantLabel, String> variantSkus = new EnumMap<>(VariantLabel.class);
        for (VariantLabel label : allowed) {
            variantSkus.put(label, ProductSkuGenerator.variantSku(sku, label));
        }
        return new SkuSuggestionResponse(sku, skuGenerator.prefix(category, name.trim()), category.getName(),
                category.getCategoryGroup(), allowed, variantSkus);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Product product = findById(id);
        Inventory inventory = findInventory(product.getId());
        if (inventory.getQuantityOnHand().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidOperationException("Cannot delete a product that still has stock on hand");
        }
        String imageUrl = product.getImageUrl();
        inventoryRepository.delete(inventory);
        productRepository.delete(product);
        productRepository.flush();
        if (imageUrl != null) {
            fileStorageService.delete(imageUrl);
        }
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

    private SkuMode resolveSkuMode(SkuMode requested, String sku) {
        if (requested != null) {
            return requested;
        }
        return sku == null || sku.isBlank() ? SkuMode.GENERATE : SkuMode.MANUAL;
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
        List<ProductVariantResponse> variants = variantRepository
                .findByProductIdOrderBySortOrderAscNameAsc(product.getId()).stream()
                .map(variantMapper::toResponse)
                .toList();
        List<ProductExtraResponse> extras = productExtraRepository
                .findByProductIdOrderBySortOrderAscId(product.getId()).stream()
                .map(productExtraMapper::toResponse)
                .toList();
        return productMapper.toResponse(product, inventory, variants, extras);
    }

    private Page<ProductResponse> toResponsePage(Page<Product> products, boolean orderableOnly) {
        List<UUID> productIds = products.stream().map(Product::getId).toList();

        Map<UUID, List<ProductVariantResponse>> variantsByProduct = new HashMap<>();
        List<ProductVariant> variants = orderableOnly
                ? variantRepository.findByProductIdInAndStatusOrderBySortOrderAscNameAsc(productIds, Status.ACTIVE)
                : variantRepository.findByProductIdInOrderBySortOrderAscNameAsc(productIds);
        for (ProductVariant variant : variants) {
            variantsByProduct.computeIfAbsent(variant.getProduct().getId(), id -> new ArrayList<>())
                    .add(variantMapper.toResponse(variant));
        }

        Map<UUID, List<ProductExtraResponse>> extrasByProduct = new HashMap<>();
        List<ProductExtra> productExtras = orderableOnly
                ? productExtraRepository.findByProductIdInAndStatusOrderBySortOrderAscId(productIds, Status.ACTIVE)
                : productExtraRepository.findByProductIdInOrderBySortOrderAscId(productIds);
        for (ProductExtra productExtra : productExtras) {
            if (orderableOnly && productExtra.getExtra().getStatus() != Status.ACTIVE) {
                continue;
            }
            extrasByProduct.computeIfAbsent(productExtra.getProduct().getId(), id -> new ArrayList<>())
                    .add(productExtraMapper.toResponse(productExtra));
        }

        return products.map(product -> productMapper.toResponse(product, findInventory(product.getId()),
                variantsByProduct.getOrDefault(product.getId(), List.of()),
                extrasByProduct.getOrDefault(product.getId(), List.of())));
    }
}
