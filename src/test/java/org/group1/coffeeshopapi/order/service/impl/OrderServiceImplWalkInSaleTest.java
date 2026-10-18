package org.group1.coffeeshopapi.order.service.impl;

import org.group1.coffeeshopapi.bakong.BakongApiClient;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.bakong.BakongQrService;
import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.properties.BakongProperties;
import org.group1.coffeeshopapi.common.properties.ShopLocationProperties;
import org.group1.coffeeshopapi.extra.entity.Extra;
import org.group1.coffeeshopapi.extra.entity.ProductExtra;
import org.group1.coffeeshopapi.extra.repository.ExtraRepository;
import org.group1.coffeeshopapi.extra.repository.ProductExtraRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.order.dto.request.OrderItemRequest;
import org.group1.coffeeshopapi.order.dto.request.StaffCreateOrderRequest;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.entity.OrderItem;
import org.group1.coffeeshopapi.order.entity.OrderItemExtra;
import org.group1.coffeeshopapi.order.mapper.OrderAuditLogMapper;
import org.group1.coffeeshopapi.order.mapper.OrderMapper;
import org.group1.coffeeshopapi.order.repository.OrderAuditLogRepository;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.realtime.ResourceChangePublisher;
import org.group1.coffeeshopapi.telegram.service.TelegramInvoiceService;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ResourceChangePublisher resourceChangePublisher;
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

    @Test
    void aPosSaleKeepsEachDrinksChoicesAndChargesItsExtras() {
        Category drinks = new Category();
        drinks.setCategoryGroup(CategoryGroup.FRESH_DRINK);
        Product latte = product("Iced Latte", drinks);
        ProductVariant large = variant(latte, VariantLabel.LARGE, "3.00");

        Extra shot = new Extra();
        shot.setId(UUID.randomUUID());
        shot.setName("Extra Shot");
        shot.setPrice(new BigDecimal("0.50"));
        ProductExtra attached = new ProductExtra();
        attached.setProduct(latte);
        attached.setExtra(shot);

        Category bakery = new Category();
        bakery.setCategoryGroup(CategoryGroup.SNACK);
        Product croissant = product("Croissant", bakery);
        ProductVariant piece = variant(croissant, VariantLabel.PIECE, "2.75");

        when(productRepository.findById(latte.getId())).thenReturn(Optional.of(latte));
        when(productRepository.findById(croissant.getId())).thenReturn(Optional.of(croissant));
        when(variantRepository.findByIdAndProductId(large.getId(), latte.getId())).thenReturn(Optional.of(large));
        when(variantRepository.findByIdAndProductId(piece.getId(), croissant.getId())).thenReturn(Optional.of(piece));
        when(productExtraRepository.findByProductIdAndExtraIdInAndStatus(latte.getId(), List.of(shot.getId()), Status.ACTIVE))
                .thenReturn(List.of(attached));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Exactly what the admin POS sends: every line names its size; drinks add their choices and extras.
        StaffCreateOrderRequest request = new StaffCreateOrderRequest(List.of(
                new OrderItemRequest(latte.getId(), 2, large.getId(), null,
                        SugarLevel.LESS, IceLevel.NO_ICE, MilkType.EXTRA, List.of(shot.getId())),
                new OrderItemRequest(croissant.getId(), 1, piece.getId(), null, null, null, null, null)), null);

        service.create(request, UUID.randomUUID());

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        Order order = saved.getValue();
        assertThat(order.getItems()).hasSize(2);

        OrderItem drink = order.getItems().get(0);
        assertThat(drink.getVariant()).isSameAs(large);
        assertThat(drink.getSugarLevel()).isEqualTo(SugarLevel.LESS);
        assertThat(drink.getIceLevel()).isEqualTo(IceLevel.NO_ICE);
        assertThat(drink.getMilkType()).isEqualTo(MilkType.EXTRA);
        assertThat(drink.getExtras()).extracting(OrderItemExtra::getExtraName).containsExactly("Extra Shot");
        assertThat(drink.getUnitPrice()).isEqualByComparingTo("3.50");
        assertThat(drink.getSubtotal()).isEqualByComparingTo("7.00");

        OrderItem snack = order.getItems().get(1);
        assertThat(snack.getVariant()).isSameAs(piece);
        assertThat(snack.getSugarLevel()).isNull();
        assertThat(snack.getExtras()).isEmpty();
        assertThat(snack.getUnitPrice()).isEqualByComparingTo("2.75");

        assertThat(order.getTotalAmount()).isEqualByComparingTo("9.75");
    }

    private Product product(String name, Category category) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        product.setCategory(category);
        return product;
    }

    private ProductVariant variant(Product product, VariantLabel label, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setProduct(product);
        variant.setName(label);
        variant.setPrice(new BigDecimal(price));
        variant.setStatus(Status.ACTIVE);
        return variant;
    }
}
