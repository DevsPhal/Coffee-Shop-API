package org.group1.coffeeshopapi.product.service.impl;

import org.group1.coffeeshopapi.admin.entity.Admin;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.extra.mapper.ProductExtraMapper;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.product.dto.request.UpdateProductRequest;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.mapper.ProductMapper;
import org.group1.coffeeshopapi.product.mapper.ProductVariantMapper;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Covers updating a product's SKU — it could be set at creation but never corrected afterward
// until this field was added to the update request.
@ExtendWith(MockitoExtension.class)
class ProductServiceImplUpdateTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductExtraRepository productExtraRepository;
    @Mock private ProductMapper productMapper;
    @Mock private ProductVariantMapper variantMapper;
    @Mock private ProductExtraMapper productExtraMapper;
    @Mock private FileStorageService fileStorageService;
    @InjectMocks private ProductServiceImpl service;

    @Test
    void updatingSkuChangesItWhenNoOtherProductHasIt() {
        Product product = productWithSku("OLD-SKU");
        stubLookups(product);
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("NEW-SKU", product.getId())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(product.getId(), updateSkuOnly("NEW-SKU"), admin());

        assertThat(product.getSku()).isEqualTo("NEW-SKU");
    }

    @Test
    void updatingSkuToOneAlreadyUsedByAnotherProductIsRejected() {
        Product product = productWithSku("OLD-SKU");
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("TAKEN-SKU", product.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.update(product.getId(), updateSkuOnly("TAKEN-SKU"), admin()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU");
        assertThat(product.getSku()).isEqualTo("OLD-SKU");
    }

    @Test
    void leavingSkuNullLeavesItUnchanged() {
        Product product = productWithSku("OLD-SKU");
        stubLookups(product);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductRequest request = new UpdateProductRequest(
                null, null, null, null, null, null, null, null, null, null);
        service.update(product.getId(), request, admin());

        assertThat(product.getSku()).isEqualTo("OLD-SKU");
    }

    private void stubLookups(Product product) {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));
        when(variantRepository.findByProductIdOrderBySortOrderAscNameAsc(product.getId())).thenReturn(List.of());
        when(productExtraRepository.findByProductIdOrderBySortOrderAscId(product.getId())).thenReturn(List.of());
    }

    private UpdateProductRequest updateSkuOnly(String sku) {
        return new UpdateProductRequest(null, null, null, sku, null, null, null, null, null, null);
    }

    private Product productWithSku(String sku) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Green Tea");
        product.setSku(sku);
        return product;
    }

    private Admin admin() {
        Admin admin = new Admin();
        admin.setId(UUID.randomUUID());
        return admin;
    }
}
