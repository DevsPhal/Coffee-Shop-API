package org.group1.coffeeshopapi.extra.service.impl;

import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.extra.dto.request.AttachProductExtraRequest;
import org.group1.coffeeshopapi.extra.entity.Extra;
import org.group1.coffeeshopapi.extra.mapper.ProductExtraMapper;
import org.group1.coffeeshopapi.extra.repository.ExtraRepository;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the one-attachment-per-product-per-extra uniqueness rule — untested before. See
 * ProductExtraServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class ProductExtraServiceImplTest {

    @Mock private ProductExtraRepository productExtraRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ExtraRepository extraRepository;
    @Mock private ProductExtraMapper productExtraMapper;
    @InjectMocks private ProductExtraServiceImpl service;

    @Test
    void attachingAnExtraThatIsAlreadyOfferedOnTheSameProductIsRejected() {
        UUID productId = UUID.randomUUID();
        UUID extraId = UUID.randomUUID();
        Product product = new Product();
        product.setName("Green Tea");
        Extra extra = new Extra();
        extra.setName("Pearl");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(extraRepository.findById(extraId)).thenReturn(Optional.of(extra));
        when(productExtraRepository.existsByProductIdAndExtraId(productId, extraId)).thenReturn(true);

        assertThatThrownBy(() -> service.attach(productId, new AttachProductExtraRequest(extraId, null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Pearl")
                .hasMessageContaining("Green Tea");
        verify(productExtraRepository, never()).save(any());
    }
}
