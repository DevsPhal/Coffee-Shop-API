package org.group1.coffeeshopapi.cart.service.impl;

import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.cart.dto.request.AddCartItemRequest;
import org.group1.coffeeshopapi.cart.entity.Cart;
import org.group1.coffeeshopapi.cart.repository.CartRepository;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Defense-in-depth check — covers a customer still POSTing a productId they already had (a
// stale page, a bookmark) after it sold out.
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductExtraRepository productExtraRepository;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private OrderService orderService;
    @InjectMocks private CartServiceImpl service;

    @Test
    void addItemRejectsAProductThatIsOutOfStock() {
        UUID customerId = UUID.randomUUID();
        when(cartRepository.findByCustomer_Id(customerId)).thenReturn(Optional.of(new Cart()));

        Product product = activeProduct();
        Inventory inventory = new Inventory();
        inventory.setQuantityOnHand(BigDecimal.ZERO);
        when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));

        var request = new AddCartItemRequest(product.getId(), 1, null, null, null, null, List.of());

        assertThatThrownBy(() -> service.addItem(customerId, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Green Tea")
                .hasMessageContaining("out of stock");
        verifyNoInteractions(variantRepository, productExtraRepository);
    }

    @Test
    void addItemRejectsAProductWithNoInventoryRowAtAll() {
        UUID customerId = UUID.randomUUID();
        when(cartRepository.findByCustomer_Id(customerId)).thenReturn(Optional.of(new Cart()));

        Product product = activeProduct();
        when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.empty());

        var request = new AddCartItemRequest(product.getId(), 1, null, null, null, null, List.of());

        assertThatThrownBy(() -> service.addItem(customerId, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("out of stock");
    }

    @Test
    void addItemRejectsAProductWhoseCategoryIsHidden() {
        UUID customerId = UUID.randomUUID();
        when(cartRepository.findByCustomer_Id(customerId)).thenReturn(Optional.of(new Cart()));

        Product product = activeProduct();
        product.getCategory().setStatus(Status.INACTIVE);

        var request = new AddCartItemRequest(product.getId(), 1, null, null, null, null, List.of());

        assertThatThrownBy(() -> service.addItem(customerId, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("not available");
        verifyNoInteractions(inventoryRepository);
    }

    private Product activeProduct() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Green Tea");
        product.setStatus(Status.ACTIVE);
        Category category = new Category();
        category.setStatus(Status.ACTIVE);
        product.setCategory(category);
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        return product;
    }
}
