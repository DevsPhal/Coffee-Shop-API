package org.group1.coffeeshopapi.product.mapper;

import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.extra.dto.response.ProductExtraResponse;
import org.group1.coffeeshopapi.product.dto.response.CustomerProductResponse;
import org.group1.coffeeshopapi.product.dto.response.ProductResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// toResponse() itself is plain field-for-field mapping, not worth a test of its own — this only
// covers toCustomerResponse's one bit of real logic: hiding an out-of-stock extra from the
// customer view (see Extra.quantityOnHand / ProductMapper#inStockOnly).
class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapperImpl();

    @Test
    void hidesAnOutOfStockExtraButKeepsUntrackedAndInStockOnes() {
        ProductExtraResponse outOfStock = extraResponse("Pearl", BigDecimal.ZERO);
        ProductExtraResponse inStock = extraResponse("Jelly", new BigDecimal("10"));
        ProductExtraResponse untracked = extraResponse("Whipped Cream", null);

        CustomerProductResponse customerResponse =
                mapper.toCustomerResponse(productResponse(List.of(outOfStock, inStock, untracked)));

        assertThat(customerResponse.extras())
                .extracting(ProductExtraResponse::name)
                .containsExactly("Jelly", "Whipped Cream");
    }

    private ProductExtraResponse extraResponse(String name, BigDecimal quantityOnHand) {
        return new ProductExtraResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                name, new BigDecimal("0.50"), 1, Status.ACTIVE, quantityOnHand);
    }

    private ProductResponse productResponse(List<ProductExtraResponse> extras) {
        return new ProductResponse(
                UUID.randomUUID(), "Green Tea", null, null, "SKU-1",
                StockUnit.PACK, SellUnit.CUP, BigDecimal.ONE,
                UUID.randomUUID(), "Drinks", null,
                Status.ACTIVE, new BigDecimal("10"), BigDecimal.ZERO,
                null, null, null, null, false,
                List.of(), extras,
                null, null, null, null, null, null, null, null);
    }
}
