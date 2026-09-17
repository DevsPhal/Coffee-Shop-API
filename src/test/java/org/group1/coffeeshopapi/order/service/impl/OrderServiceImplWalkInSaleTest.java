package org.group1.coffeeshopapi.order.service.impl;

import org.group1.coffeeshopapi.bakong.BakongApiClient;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.bakong.BakongQrService;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.common.properties.ShopLocationProperties;
import org.group1.coffeeshopapi.extra.repository.ExtraRepository;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.order.dto.request.OrderItemRequest;
import org.group1.coffeeshopapi.order.dto.request.StaffCreateOrderRequest;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.mapper.OrderAuditLogMapper;
import org.group1.coffeeshopapi.order.mapper.OrderMapper;
import org.group1.coffeeshopapi.order.repository.OrderAuditLogRepository;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.telegram.service.TelegramInvoiceService;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// A walk-in sale rung up by staff is always served in person at the counter — confirms it lands
// as a plain pickup order instead of silently defaulting to something else.
@ExtendWith(MockitoExtension.class)
class OrderServiceImplWalkInSaleTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderAuditLogRepository orderAuditLogRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductExtraRepository productExtraRepository;
    @Mock private ExtraRepository extraRepository;
    @Mock private InventoryService inventoryService;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderAuditLogMapper orderAuditLogMapper;
    @Mock private BakongQrService bakongQrService;
    @Mock private BakongApiClient bakongApiClient;
    @Mock private BakongExchangeRateService bakongExchangeRateService;
    @Mock private TelegramInvoiceService telegramInvoiceService;
    @Mock private CustomerRepository customerRepository;
    @Mock private ActorLookupService actorLookupService;
    @Mock private ShopLocationProperties shopLocationProperties;
    @Mock private BakongProperties bakongProperties;
    @InjectMocks private OrderServiceImpl service;

    @Test
    void ringingUpAWalkInSaleAlwaysProducesAPickupOrderHandledByTheStaffMemberWhoRangItUp() {
        Category category = new Category();
        category.setCategoryGroup(CategoryGroup.SNACK);

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Croissant");
        product.setCategory(category);

        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setProduct(product);
        variant.setName(VariantLabel.PIECE);
        variant.setPrice(new BigDecimal("2.50"));
        variant.setStatus(Status.ACTIVE);

        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdAndStatusOrderBySortOrderAscNameAsc(product.getId(), Status.ACTIVE))
                .thenReturn(List.of(variant));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID baristaId = UUID.randomUUID();
        StaffCreateOrderRequest request = new StaffCreateOrderRequest(
                List.of(new OrderItemRequest(product.getId(), 2, null, null, null, null, null, null)), null);

        service.create(request, baristaId);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        Order order = saved.getValue();
        assertThat(order.getFulfillmentMethod()).isEqualTo(FulfillmentMethod.PICKUP);
        assertThat(order.isDelivery()).isFalse();
        assertThat(order.getDeliveryAddress()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getHandledBy()).isEqualTo(baristaId);
    }
}
