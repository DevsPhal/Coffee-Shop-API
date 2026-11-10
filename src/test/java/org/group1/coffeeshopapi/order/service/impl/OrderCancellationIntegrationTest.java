package org.group1.coffeeshopapi.order.service.impl;

import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.OrderType;
import org.group1.coffeeshopapi.common.enums.QuantityUnit;
import org.group1.coffeeshopapi.common.enums.RegisterType;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.StockMovementReason;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.exception.ForbiddenOperationException;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.inventory.dto.request.StockInRequest;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.inventory.repository.StockMovementRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.order.dto.request.CancelOrderRequest;
import org.group1.coffeeshopapi.order.dto.request.CashPaymentRequest;
import org.group1.coffeeshopapi.order.dto.request.CheckoutDetailsRequest;
import org.group1.coffeeshopapi.order.dto.request.CreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.request.OrderItemRequest;
import org.group1.coffeeshopapi.order.dto.request.StaffCreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Cancelling orders the way the café does, against the real schema: who may cancel at each step, the reason,
// the stock coming back (or written off), and a refunded sale leaving the sales figures. Rolled back after each test.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderCancellationIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private InventoryService inventoryService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private CustomerRepository customerRepository;

    private final UUID barista = UUID.randomUUID();
    private final UUID otherBarista = UUID.randomUUID();
    private final UUID admin = UUID.randomUUID();
    private Product croissant;
    private ProductVariant piece;

    @BeforeEach
    void stockTwentyCroissants() {
        Category bakery = new Category();
        bakery.setName("Bakery " + UUID.randomUUID());
        bakery.setCategoryGroup(CategoryGroup.SNACK);
        bakery = categoryRepository.save(bakery);
        croissant = new Product();
        croissant.setName("Croissant");
        croissant.setSku("IT-" + UUID.randomUUID());
        croissant.setCategory(bakery);
        croissant.setStockUnit(StockUnit.PACK);
        croissant.setSellUnit(SellUnit.PIECE);
        croissant.setUnitsPerStock(BigDecimal.ONE);
        croissant = productRepository.save(croissant);
        Inventory inventory = new Inventory();
        inventory.setProduct(croissant);
        inventoryRepository.save(inventory);
        piece = new ProductVariant();
        piece.setProduct(croissant);
        piece.setName(VariantLabel.PIECE);
        piece.setPrice(new BigDecimal("2.50"));
        piece.setUnitsPerSale(1);
        piece = variantRepository.save(piece);
        inventoryService.stockIn(new StockInRequest(croissant.getId(), new BigDecimal("20"), QuantityUnit.PURCHASE,
                new BigDecimal("1.00"), null), admin);
    }

    @Test
    void aCustomerCanCancelTheirCashOrderWhileItIsBeingMadeAndTheStockComesBack() {
        Customer dara = customer();
        OrderResponse order = customerPickupOrder(dara, 2);
        orderService.selectCashOnPickup(order.id(), dara.getId());
        orderService.startPreparing(order.id(), barista);
        assertThat(onHand()).isEqualByComparingTo("18");

        assertThatThrownBy(() -> orderService.cancelForCustomer(order.id(), dara.getId(), new CancelOrderRequest("  ")))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("reason");

        OrderResponse cancelled = orderService.cancelForCustomer(order.id(), dara.getId(),
                new CancelOrderRequest(" Changed my mind "));

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.cancelReason()).isEqualTo("Changed my mind");
        assertThat(cancelled.cancelledByRole()).isEqualTo(Role.CUSTOMER);
        assertThat(cancelled.cancelledAt()).isNotNull();
        assertThat(cancelled.refundAmount()).isNull();
        assertThat(onHand()).isEqualByComparingTo("20");
        assertThatThrownBy(() -> orderService.cancelForCustomer(order.id(), dara.getId(), new CancelOrderRequest("Again")))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void theBaristaHandlingACashOrderCanCancelItAndWriteTheMadeItemsOffAsWaste() {
        Customer dara = customer();
        OrderResponse order = customerPickupOrder(dara, 3);
        orderService.selectCashOnPickup(order.id(), dara.getId());
        orderService.startPreparing(order.id(), barista);

        assertThatThrownBy(() -> orderService.cancel(order.id(), otherBarista, new CancelOrderRequest("No-show")))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Another staff member");

        OrderResponse cancelled = orderService.cancel(order.id(), barista,
                new CancelOrderRequest("Customer didn't come to collect", false));

        assertThat(cancelled.cancelledByRole()).isEqualTo(Role.BARISTA);
        // Made, so written off: the stock stays out, and the sale is now waste.
        assertThat(onHand()).isEqualByComparingTo("17");
        assertThat(stockMovementRepository.findByOrderIdAndReason(order.id(), StockMovementReason.WASTE)).hasSize(1);
        assertThat(stockMovementRepository.findByOrderIdAndReason(order.id(), StockMovementReason.RETURN)).hasSize(1);
    }

    @Test
    void aWaitingOrderNobodyHasTakenCanBeCancelledByAnyBarista() {
        Customer dara = customer();
        OrderResponse order = customerPickupOrder(dara, 1);
        orderService.selectCashOnPickup(order.id(), dara.getId());

        OrderResponse cancelled = orderService.cancel(order.id(), otherBarista, new CancelOrderRequest("Called to cancel"));

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(onHand()).isEqualByComparingTo("20");
    }

    @Test
    void aPaidCashOrderIsARefundOnlyAnAdminCanMakeAndItLeavesTheSales() {
        OrderResponse sale = orderService.create(new StaffCreateOrderRequest(
                List.of(new OrderItemRequest(croissant.getId(), 2, piece.getId(), null, null, null, null, null)),
                null, OrderType.TAKE_AWAY, null), barista);
        orderService.payCash(sale.id(), barista, new CashPaymentRequest(Currency.KHR, new BigDecimal("25000"), null));
        Order paid = orderRepository.findById(sale.id()).orElseThrow();
        LocalDateTime start = paid.getPaidAt().minusMinutes(1);
        LocalDateTime end = paid.getPaidAt().plusMinutes(1);
        assertThat(orderRepository.findPaidInRange(start, end)).extracting(Order::getId).contains(sale.id());
        assertThat(onHand()).isEqualByComparingTo("18");

        assertThatThrownBy(() -> orderService.cancel(sale.id(), barista, new CancelOrderRequest("Wrong item")))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Only an admin");

        OrderResponse refunded = orderService.cancelAny(sale.id(), admin, new CancelOrderRequest("Wrong item rung up"));

        // $5.00 paid in riel at the shop rate: the riel taken in goes back, rounded like the receipt.
        assertThat(refunded.refundCurrency()).isEqualTo(Currency.KHR);
        assertThat(refunded.refundAmount()).isEqualByComparingTo("20500");
        assertThat(refunded.cancelledByRole()).isEqualTo(Role.ADMIN);
        assertThat(onHand()).isEqualByComparingTo("20");
        assertThat(orderRepository.findPaidInRange(start, end)).extracting(Order::getId).doesNotContain(sale.id());
    }

    @Test
    void aCustomerCannotCancelAnOrderOnItsWayButTheStaffCan() {
        Customer dara = customer();
        OrderResponse order = orderService.createForCustomer(new CreateOrderRequest(
                List.of(new OrderItemRequest(croissant.getId(), 1, piece.getId(), null, null, null, null, null)), null,
                new CheckoutDetailsRequest(FulfillmentMethod.DELIVERY, "Dara", "012 345 678", "#12, St. 590")),
                dara.getId(), new BigDecimal("11.5564"), new BigDecimal("104.9282"));
        orderService.setDeliveryFee(order.id(), new BigDecimal("1.00"), admin);
        orderService.selectCashOnPickup(order.id(), dara.getId());
        orderService.startPreparing(order.id(), barista);
        orderService.dispatchForDelivery(order.id(), barista);

        assertThatThrownBy(() -> orderService.cancelForCustomer(order.id(), dara.getId(), new CancelOrderRequest("Too slow")))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("on its way");

        OrderResponse cancelled = orderService.cancel(order.id(), barista,
                new CancelOrderRequest("Customer refused at the door", true));

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(onHand()).isEqualByComparingTo("20");
    }

    @Test
    void aFinishedOrderCannotBeCancelled() {
        OrderResponse sale = orderService.create(new StaffCreateOrderRequest(
                List.of(new OrderItemRequest(croissant.getId(), 1, piece.getId(), null, null, null, null, null)),
                null, OrderType.TAKE_AWAY, null), barista);
        orderService.payCash(sale.id(), barista, new CashPaymentRequest(Currency.USD, new BigDecimal("5.00"), null));
        orderService.startPreparing(sale.id(), barista);
        orderService.completePickup(sale.id(), barista);

        assertThatThrownBy(() -> orderService.cancelAny(sale.id(), admin, new CancelOrderRequest("Late change")))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already completed");
    }

    private OrderResponse customerPickupOrder(Customer customer, int quantity) {
        return orderService.createForCustomer(new CreateOrderRequest(
                List.of(new OrderItemRequest(croissant.getId(), quantity, piece.getId(), null, null, null, null, null)), null,
                new CheckoutDetailsRequest(FulfillmentMethod.PICKUP, customer.getFullName(), null, null)),
                customer.getId(), null, null);
    }

    private BigDecimal onHand() {
        return inventoryRepository.findByProductIdIn(List.of(croissant.getId())).getFirst().getQuantityOnHand();
    }

    private Customer customer() {
        Customer customer = new Customer();
        customer.setFullName("Dara Sok");
        customer.setEmail(UUID.randomUUID() + "@example.test");
        customer.setPassword("unused");
        customer.setStatus(UserStatus.ACTIVE);
        customer.setRegisterType(RegisterType.EMAIL);
        return customerRepository.saveAndFlush(customer);
    }
}
