package org.group1.coffeeshopapi.product.entity;

import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.Status;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    void sellingOneCanOfATwentyFourCanCartonUsesAFractionOfACarton() {
        Product product = new Product();
        product.setUnitsPerStock(new BigDecimal("24"));

        assertThat(product.toStockQuantity(1)).isEqualByComparingTo("0.042");
        assertThat(product.toStockQuantity(3)).isEqualByComparingTo("0.125");
        assertThat(product.toStockQuantity(24)).isEqualByComparingTo("1");
    }

    @Test
    void aOneToOneProductCutsExactlyWhatWasSold() {
        Product product = new Product();

        assertThat(product.toStockQuantity(2)).isEqualByComparingTo("2");
    }

    @Test
    void aPercentageDiscountIsRoundedToCents() {
        Product product = new Product();
        product.setDiscountType(DiscountType.PERCENTAGE);
        product.setDiscountValue(new BigDecimal("12.5"));

        BigDecimal price = product.getFinalPrice(new BigDecimal("1.35"), LocalDateTime.now());

        // 1.35 - 12.5% = 1.18125, stored as 1.18
        assertThat(price).isEqualByComparingTo("1.18");
        assertThat(price.scale()).isEqualTo(2);
    }

    @Test
    void aFixedDiscountNeverGoesBelowZero() {
        Product product = new Product();
        product.setDiscountType(DiscountType.FIXED);
        product.setDiscountValue(new BigDecimal("5"));

        assertThat(product.getFinalPrice(new BigDecimal("1.00"), LocalDateTime.now())).isEqualByComparingTo("0");
    }

    @Test
    void isOnlyForSaleWhileBothProductAndCategoryAreActive() {
        Category category = new Category();
        category.setStatus(Status.ACTIVE);
        Product product = new Product();
        product.setCategory(category);

        assertThat(product.isAvailableForSale()).isTrue();

        category.setStatus(Status.INACTIVE);
        assertThat(product.isAvailableForSale()).isFalse();

        category.setStatus(Status.ACTIVE);
        product.setStatus(Status.INACTIVE);
        assertThat(product.isAvailableForSale()).isFalse();
    }
}
