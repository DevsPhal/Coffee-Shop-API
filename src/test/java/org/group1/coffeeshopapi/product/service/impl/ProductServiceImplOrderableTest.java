package org.group1.coffeeshopapi.product.service.impl;

import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.storage.FileStorageService;
import org.group1.coffeeshopapi.extra.entity.Extra;
import org.group1.coffeeshopapi.extra.entity.ProductExtra;
import org.group1.coffeeshopapi.extra.mapper.ProductExtraMapper;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.mapper.ProductMapper;
import org.group1.coffeeshopapi.product.mapper.ProductVariantMapper;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// The customer's single-product view must follow the same rules as the menu list.
@ExtendWith(MockitoExtension.class)
class ProductServiceImplOrderableTest {

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
    void aProductInAHiddenCategoryIsNotFoundForCustomers() {
        Product product = product(Status.INACTIVE);
        stubStock(product, "5");

        assertThatThrownBy(() -> service.getOrderableById(product.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void aSoldOutProductIsNotFoundForCustomers() {
        Product product = product(Status.ACTIVE);
        stubStock(product, "0");

        assertThatThrownBy(() -> service.getOrderableById(product.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void customersOnlySeeActiveSizesAndExtrasThatAreSwitchedOnGlobally() {
        Product product = product(Status.ACTIVE);
        stubStock(product, "5");
        Extra switchedOff = new Extra();
        switchedOff.setStatus(Status.INACTIVE);
        ProductExtra attached = new ProductExtra();
        attached.setProduct(product);
        attached.setExtra(switchedOff);
        when(productExtraRepository.findByProductIdInAndStatusOrderBySortOrderAscId(anyList(), eq(Status.ACTIVE)))
                .thenReturn(List.of(attached));

        service.getOrderableById(product.getId());

        verify(variantRepository).findByProductIdInAndStatusOrderBySortOrderAscNameAsc(anyList(), eq(Status.ACTIVE));
        verify(productExtraMapper, never()).toResponse(any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<org.group1.coffeeshopapi.extra.dto.response.ProductExtraResponse>> extras =
                ArgumentCaptor.forClass(List.class);
        verify(productMapper).toResponse(eq(product), any(Inventory.class), anyList(), extras.capture());
        assertThat(extras.getValue()).isEmpty();
    }

    @Test
    void theAdminListShowsInactiveSizesAndExtrasToo() {
        Product product = product(Status.ACTIVE);
        when(productRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(product)));
        stubStock(product, "5");

        service.list(null, org.springframework.data.domain.Pageable.unpaged());

        verify(variantRepository).findByProductIdInOrderBySortOrderAscNameAsc(anyList());
        verify(productExtraRepository).findByProductIdInOrderBySortOrderAscId(anyList());
    }

    private Product product(Status categoryStatus) {
        Category category = new Category();
        category.setStatus(categoryStatus);
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Iced Green Tea");
        product.setCategory(category);
        org.mockito.Mockito.lenient().when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        return product;
    }

    private void stubStock(Product product, String quantity) {
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantityOnHand(new BigDecimal(quantity));
        when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));
    }
}
