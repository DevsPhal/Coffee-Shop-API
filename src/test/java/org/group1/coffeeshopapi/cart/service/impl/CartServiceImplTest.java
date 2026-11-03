package org.group1.coffeeshopapi.cart.service.impl;

import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.cart.dto.request.AddCartItemRequest;
import org.group1.coffeeshopapi.cart.entity.Cart;
import org.group1.coffeeshopapi.cart.repository.CartRepository;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.inventory.service.VariantStock;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductExtraRepository productExtraRepository;
    @Mock private InventoryService inventoryService;
    @Mock private CustomerRepository customerRepository;
    @Mock private OrderService orderService;
    @InjectMocks private CartServiceImpl service;

    @Test
    void addItemRejectsAProductThatIsOutOfStock() {
        UUID customerId = UUID.randomUUID();
        when(cartRepository.findByCustomer_Id(customerId)).thenReturn(Optional.of(new Cart()));

        Product product = activeProduct();
        ProductVariant onlySize = onlySizeOf(product);
        when(inventoryService.variantStock(List.of(onlySize)))
                .thenReturn(Map.of(onlySize.getId(), new VariantStock(0, false)));

        var request = new AddCartItemRequest(product.getId(), 1, null, null, null, null, List.of());

        assertThatThrownBy(() -> service.addItem(customerId, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Green Tea")
                .hasMessageContaining("out of stock");
        verifyNoInteractions(productExtraRepository);
    }

    @Test
    void addItemRejectsADrinkWhoseRecipeIngredientsRanOut() {
        UUID customerId = UUID.randomUUID();
        when(cartRepository.findByCustomer_Id(customerId)).thenReturn(Optional.of(new Cart()));

        Product product = activeProduct();
        ProductVariant onlySize = onlySizeOf(product);
        when(inventoryService.variantStock(List.of(onlySize)))
                .thenReturn(Map.of(onlySize.getId(), new VariantStock(0, true)));

        var request = new AddCartItemRequest(product.getId(), 1, null, null, null, null, List.of());

        assertThatThrownBy(() -> service.addItem(customerId, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("out of stock");
    }

    @Test
    void addItemRejectsAProductWithNoInventoryRowAtAll() {
        UUID customerId = UUID.randomUUID();
        when(cartRepository.findByCustomer_Id(customerId)).thenReturn(Optional.of(new Cart()));

        Product product = activeProduct();
        onlySizeOf(product);

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
        verifyNoInteractions(inventoryService);
    }

    private ProductVariant onlySizeOf(Product product) {
        ProductVariant size = new ProductVariant();
        size.setId(UUID.randomUUID());
        size.setProduct(product);
        size.setStatus(Status.ACTIVE);
        when(variantRepository.findByProductIdAndStatusOrderBySortOrderAscNameAsc(product.getId(), Status.ACTIVE))
                .thenReturn(List.of(size));
        return size;
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
