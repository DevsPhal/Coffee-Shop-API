package org.group1.coffeeshopapi.product.service;

import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.extra.entity.Extra;
import org.group1.coffeeshopapi.extra.entity.ProductExtra;
import org.group1.coffeeshopapi.product.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductExtraResolverTest {

    private final Product product = product();

    @Test
    void resolvesNothingWhenNoExtraIdsAreRequested() {
        assertThat(ProductExtraResolver.resolve(product, null, List.of())).isEmpty();
        assertThat(ProductExtraResolver.resolve(product, List.of(), List.of())).isEmpty();
    }

    @Test
    void rejectsAnExtraThatIsNotAttachedToTheProduct() {
        Extra pearl = extra("Pearl", null);
        assertThatThrownBy(() -> ProductExtraResolver.resolve(product, List.of(UUID.randomUUID()),
                List.of(attach(pearl))))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("aren't available");
    }

    @Test
    void resolvesAnAttachedUntrackedExtraAsAlwaysAvailable() {
        Extra pearl = extra("Pearl", null);
        ProductExtra attached = attach(pearl);

        List<Extra> resolved = ProductExtraResolver.resolve(product, List.of(pearl.getId()), List.of(attached));

        assertThat(resolved).containsExactly(pearl);
    }

    @Test
    void resolvesAnAttachedExtraWithPositiveStock() {
        Extra pearl = extra("Pearl", new BigDecimal("5"));
        ProductExtra attached = attach(pearl);

        List<Extra> resolved = ProductExtraResolver.resolve(product, List.of(pearl.getId()), List.of(attached));

        assertThat(resolved).containsExactly(pearl);
    }

    @Test
    void rejectsAnExtraSwitchedOffGloballyEvenIfStillAttached() {
        Extra pearl = extra("Pearl", null);
        pearl.setStatus(Status.INACTIVE);
        ProductExtra attached = attach(pearl);

        assertThatThrownBy(() -> ProductExtraResolver.resolve(product, List.of(pearl.getId()), List.of(attached)))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void rejectsAnAttachedExtraThatIsOutOfStock() {
        Extra pearl = extra("Pearl", BigDecimal.ZERO);
        ProductExtra attached = attach(pearl);

        assertThatThrownBy(() -> ProductExtraResolver.resolve(product, List.of(pearl.getId()), List.of(attached)))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Pearl")
                .hasMessageContaining("out of stock");
    }

    @Test
    void duplicateRequestedIdsResolveToOneEntry() {
        Extra pearl = extra("Pearl", null);
        ProductExtra attached = attach(pearl);

        List<Extra> resolved = ProductExtraResolver.resolve(
                product, List.of(pearl.getId(), pearl.getId()), List.of(attached));

        assertThat(resolved).containsExactly(pearl);
    }

    private Extra extra(String name, BigDecimal quantityOnHand) {
        Extra extra = new Extra();
        extra.setId(UUID.randomUUID());
        extra.setName(name);
        extra.setQuantityOnHand(quantityOnHand);
        return extra;
    }

    private ProductExtra attach(Extra extra) {
        ProductExtra productExtra = new ProductExtra();
        productExtra.setProduct(product);
        productExtra.setExtra(extra);
        return productExtra;
    }

    private Product product() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Green Tea");
        return product;
    }
}
