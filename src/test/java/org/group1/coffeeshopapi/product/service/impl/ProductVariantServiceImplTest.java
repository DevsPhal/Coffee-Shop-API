package org.group1.coffeeshopapi.product.service.impl;

import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                new CreateProductVariantRequest(VariantLabel.LARGE, new BigDecimal("3.00"), null, null)))
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
                new UpdateProductVariantRequest(VariantLabel.LARGE, null, null, null, null)))
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
                new UpdateProductVariantRequest(VariantLabel.MEDIUM, new BigDecimal("2.50"), null, null, null)))
                .doesNotThrowAnyException();
        verify(variantRepository, never()).existsByProductIdAndName(any(), any());
    }

    @Test
    void aSnackCannotGetADrinkSizeVariant() {
        UUID productId = UUID.randomUUID();
        Category snacks = new Category();
        snacks.setCategoryGroup(CategoryGroup.SNACK);
        Product croissant = new Product();
        croissant.setName("Croissant");
        croissant.setCategory(snacks);
        when(productRepository.findById(productId)).thenReturn(Optional.of(croissant));

        assertThatThrownBy(() -> service.create(productId,
                new CreateProductVariantRequest(VariantLabel.LARGE, new BigDecimal("3.00"), null, null)))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("PIECE");
        verify(variantRepository, never()).save(any());
    }

    @Test
    void aCartonVariantSellsTheWholeCartonOfCansByDefault() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(cokeBoughtByTheCartonOf24()));
        when(variantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(productId, new CreateProductVariantRequest(VariantLabel.CARTON, new BigDecimal("16.00"), null, null));

        ArgumentCaptor<ProductVariant> saved = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).save(saved.capture());
        assertThat(saved.getValue().getUnitsPerSale()).isEqualTo(24);
    }

    @Test
    void aSingleCanVariantSellsOneCan() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(cokeBoughtByTheCartonOf24()));
        when(variantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(productId, new CreateProductVariantRequest(VariantLabel.MEDIUM, new BigDecimal("0.75"), null, null));

        ArgumentCaptor<ProductVariant> saved = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).save(saved.capture());
        assertThat(saved.getValue().getUnitsPerSale()).isEqualTo(1);
    }

    @Test
    void aPackSizeOtherThanTheStockUnitNeedsItsCountGiven() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(cokeBoughtByTheCartonOf24()));

        assertThatThrownBy(() -> service.create(productId,
                new CreateProductVariantRequest(VariantLabel.PACK, new BigDecimal("4.20"), null, null)))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("how many CAN");
        verify(variantRepository, never()).save(any());
    }

    @Test
    void aSixPackOfACartonProductIsSavedWithTheCountGiven() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(cokeBoughtByTheCartonOf24()));
        when(variantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(productId, new CreateProductVariantRequest(VariantLabel.PACK, new BigDecimal("4.20"), 6, null));

        ArgumentCaptor<ProductVariant> saved = ArgumentCaptor.forClass(ProductVariant.class);
        verify(variantRepository).save(saved.capture());
        assertThat(saved.getValue().getUnitsPerSale()).isEqualTo(6);
    }

    @Test
    void aFreshDrinkCannotBeSoldByTheCarton() {
        UUID productId = UUID.randomUUID();
        Category freshDrinks = new Category();
        freshDrinks.setCategoryGroup(CategoryGroup.FRESH_DRINK);
        Product latte = new Product();
        latte.setName("Iced Latte");
        latte.setCategory(freshDrinks);
        when(productRepository.findById(productId)).thenReturn(Optional.of(latte));

        assertThatThrownBy(() -> service.create(productId,
                new CreateProductVariantRequest(VariantLabel.CARTON, new BigDecimal("30.00"), 24, null)))
                .isInstanceOf(InvalidOperationException.class);
        verify(variantRepository, never()).save(any());
    }

    private Product cokeBoughtByTheCartonOf24() {
        Category beverages = new Category();
        beverages.setCategoryGroup(CategoryGroup.BEVERAGE);
        Product coke = new Product();
        coke.setName("Coke");
        coke.setCategory(beverages);
        coke.setStockUnit(StockUnit.CARTON);
        coke.setSellUnit(SellUnit.CAN);
        coke.setUnitsPerStock(new BigDecimal("24"));
        return coke;
    }
}
