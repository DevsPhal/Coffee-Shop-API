package org.group1.coffeeshopapi.product.entity;

import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.enums.DiscountType;
import org.group1.coffeeshopapi.common.enums.QuantityUnit;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {

    @Test
    void cartonsAreCountedAsTheCansInThem() {
        Product product = new Product();
        product.setUnitsPerStock(new BigDecimal("24"));

        assertThat(product.toBaseUnits(new BigDecimal("2"), QuantityUnit.PURCHASE)).isEqualByComparingTo("48");
        assertThat(product.toBaseUnits(new BigDecimal("0.5"), QuantityUnit.PURCHASE)).isEqualByComparingTo("12");
    }

    @Test
    void cansAreCountedAsThemselves() {
        Product product = new Product();
        product.setUnitsPerStock(new BigDecimal("24"));

        assertThat(product.toBaseUnits(new BigDecimal("3"), QuantityUnit.BASE)).isEqualByComparingTo("3");
    }

    @Test
    void aOneToOneProductCountsWhatWasBought() {
        Product product = new Product();

        assertThat(product.toBaseUnits(new BigDecimal("2"), QuantityUnit.PURCHASE)).isEqualByComparingTo("2");
    }

    @Test
    void namesTheUnitEachQuantityIsIn() {
        Product product = new Product();
        product.setStockUnit(StockUnit.CARTON);
        product.setSellUnit(SellUnit.CAN);

        assertThat(product.unitName(QuantityUnit.PURCHASE)).isEqualTo("CARTON");
        assertThat(product.unitName(QuantityUnit.BASE)).isEqualTo("CAN");
    }

    @Test
    void aPercentageDiscountIsRoundedToCents() {
        Product product = new Product();
        product.setDiscountType(DiscountType.PERCENTAGE);
        product.setDiscountValue(new BigDecimal("12.5"));

        BigDecimal price = product.getFinalPrice(new BigDecimal("1.35"), LocalDateTime.now());

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
