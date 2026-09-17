package org.group1.coffeeshopapi.product.service.impl;

import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.product.dto.request.CreateProductVariantRequest;
import org.group1.coffeeshopapi.product.dto.request.UpdateProductVariantRequest;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.mapper.ProductVariantMapper;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the one-variant-name-per-product uniqueness rule — untested before. See
 * ProductVariantServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class ProductVariantServiceImplTest {

    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantMapper variantMapper;
    @InjectMocks private ProductVariantServiceImpl service;

    @Test
    void creatingASecondVariantWithTheSameNameOnTheSameProductIsRejected() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(new Product()));
        when(variantRepository.existsByProductIdAndName(productId, VariantLabel.LARGE)).thenReturn(true);

        assertThatThrownBy(() -> service.create(productId,
                new CreateProductVariantRequest(VariantLabel.LARGE, new BigDecimal("3.00"), null)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(variantRepository, never()).save(any());
    }

    @Test
    void renamingAVariantToOneAlreadyUsedByAnotherVariantOnTheSameProductIsRejected() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setName(VariantLabel.MEDIUM);
        when(variantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(variant));
        when(variantRepository.existsByProductIdAndName(productId, VariantLabel.LARGE)).thenReturn(true);

        assertThatThrownBy(() -> service.update(productId, variantId,
                new UpdateProductVariantRequest(VariantLabel.LARGE, null, null, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void renamingAVariantToItsOwnCurrentNameIsNotTreatedAsADuplicate() {
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        ProductVariant variant = new ProductVariant();
        variant.setId(variantId);
        variant.setName(VariantLabel.MEDIUM);
        when(variantRepository.findByIdAndProductId(variantId, productId)).thenReturn(Optional.of(variant));
        when(variantRepository.save(variant)).thenReturn(variant);

        assertThatCode(() -> service.update(productId, variantId,
                new UpdateProductVariantRequest(VariantLabel.MEDIUM, new BigDecimal("2.50"), null, null)))
                .doesNotThrowAnyException();
        verify(variantRepository, never()).existsByProductIdAndName(any(), any());
    }
}
