package org.group1.coffeeshopapi.telegram.service.impl;

import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Covers the Telegram catalog message formatting — cheapest-variant pricing with the "from"
 * prefix, discount badges, and the empty-catalog/unknown-category messages — untested before. See
 * TelegramCatalogServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class TelegramCatalogServiceImplTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductVariantRepository variantRepository;
    @InjectMocks private TelegramCatalogServiceImpl service;

    @Test
    void anEmptyMenuSaysSoInsteadOfPrintingAnEmptyList() {
        when(productRepository.findByStatusAndInStockOrderByNameAsc(Status.ACTIVE)).thenReturn(List.of());

        assertThat(service.buildMenu(null)).contains("menu is empty");
    }

    @Test
    void aProductWithSeveralVariantsShowsItsCheapestPricePrefixedWithFrom() {
        Category category = category("Drinks");
        Product product = product(category, "Green Tea");
        when(productRepository.findByStatusAndInStockOrderByNameAsc(Status.ACTIVE)).thenReturn(List.of(product));
        when(variantRepository.findByProductIdInAndStatusOrderBySortOrderAscNameAsc(any(), any())).thenReturn(List.of(
                variant(product, VariantLabel.LARGE, "3.00"),
                variant(product, VariantLabel.MEDIUM, "2.00")));

        String menu = service.buildMenu(null);

        assertThat(menu).contains("from $2.00");
    }

    @Test
    void aProductWithOneVariantShowsItsPriceWithNoFromPrefix() {
        Category category = category("Drinks");
        Product product = product(category, "Iced Coffee");
        when(productRepository.findByStatusAndInStockOrderByNameAsc(Status.ACTIVE)).thenReturn(List.of(product));
        when(variantRepository.findByProductIdInAndStatusOrderBySortOrderAscNameAsc(any(), any()))
                .thenReturn(List.of(variant(product, VariantLabel.MEDIUM, "1.75")));

        String menu = service.buildMenu(null);

        assertThat(menu).contains("$1.75").doesNotContain("from $1.75");
    }

    @Test
    void aProductWithNoPricedVariantYetShowsPriceNotSetInsteadOfCrashing() {
        Category category = category("Drinks");
        Product product = product(category, "New Item");
        when(productRepository.findByStatusAndInStockOrderByNameAsc(Status.ACTIVE)).thenReturn(List.of(product));
        when(variantRepository.findByProductIdInAndStatusOrderBySortOrderAscNameAsc(any(), any())).thenReturn(List.of());

        String menu = service.buildMenu(null);

        assertThat(menu).contains("price not set");
    }

    @Test
    void aDiscountedProductShowsAStruckThroughOriginalPriceAndAPercentageBadge() {
        Category category = category("Drinks");
        Product product = product(category, "Mocha");
        product.setDiscountType(DiscountType.PERCENTAGE);
        product.setDiscountValue(new BigDecimal("10"));
        product.setDiscountStartAt(LocalDateTime.now().minusHours(1));
        product.setDiscountEndAt(LocalDateTime.now().plusHours(1));

        when(productRepository.findByStatusAndInStockOrderByNameAsc(Status.ACTIVE)).thenReturn(List.of(product));
        when(variantRepository.findByProductIdInAndStatusOrderBySortOrderAscNameAsc(any(), any()))
                .thenReturn(List.of(variant(product, VariantLabel.MEDIUM, "2.00")));

        String discounts = service.buildDiscounts();

        assertThat(discounts).contains("<s>$2.00</s>").contains("$1.80").contains("10% OFF");
    }

    @Test
    void filteringByAnUnknownCategoryNameSaysSoInsteadOfReturningEverything() {
        when(categoryRepository.findByNameIgnoreCase("Nonexistent")).thenReturn(Optional.empty());

        assertThat(service.buildMenu("Nonexistent")).contains("No category named");
    }

    private Category category(String name) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName(name);
        category.setStatus(Status.ACTIVE);
        return category;
    }

    private Product product(Category category, String name) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setCategory(category);
        return product;
    }

    private ProductVariant variant(Product product, VariantLabel label, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setName(label);
        variant.setPrice(new BigDecimal(price));
        variant.setStatus(Status.ACTIVE);
        return variant;
    }
}
