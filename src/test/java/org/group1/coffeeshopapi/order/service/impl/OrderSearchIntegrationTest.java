package org.group1.coffeeshopapi.order.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderChannel;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.RegisterType;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.order.dto.request.OrderSearchFilter;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderStatusSummaryResponse;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.user.entity.Customer;
import org.group1.coffeeshopapi.user.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderSearchIntegrationTest {

    // Every order here is placed in this window, and every query is limited to it.
    private static final LocalDate FIRST_DAY = LocalDate.of(2001, 2, 1);
    private static final LocalDate LAST_DAY = LocalDate.of(2001, 2, 4);
    private static final Pageable NEWEST_FIRST = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CustomerRepository customerRepository;
    @PersistenceContext private EntityManager entityManager;

    private Order lateWalkInCompleted;
    private Order walkInPaid;
    private Order onlineDelivery;
    private Order walkInDeliveredToContact;
    private Order cancelledWalkIn;
    private Order onlineDineInAwaitingPayment;

    @BeforeEach
    void placeOrders() {
        Customer dara = customer("Dara Sok");
        lateWalkInCompleted = order(null, null, OrderStatus.COMPLETED, PaymentMethod.CASH, FulfillmentMethod.PICKUP,
                "2.00", FIRST_DAY.atTime(23, 59, 59));
        walkInPaid = order(null, null, OrderStatus.PAID, PaymentMethod.CASH, FulfillmentMethod.PICKUP,
                "3.00", FIRST_DAY.plusDays(1).atStartOfDay());
        onlineDelivery = order(dara, null, OrderStatus.OUT_FOR_DELIVERY, PaymentMethod.BAKONG,
                FulfillmentMethod.DELIVERY, "7.50", FIRST_DAY.plusDays(2).atTime(10, 0));
        walkInDeliveredToContact = order(null, "Bopha Chan", OrderStatus.DELIVERED, PaymentMethod.CASH,
                FulfillmentMethod.DELIVERY, "5.00", FIRST_DAY.plusDays(2).atTime(12, 0));
        cancelledWalkIn = order(null, null, OrderStatus.CANCELLED, null, FulfillmentMethod.PICKUP,
                "1.50", LAST_DAY.atTime(7, 0));
        onlineDineInAwaitingPayment = order(dara, null, OrderStatus.PENDING, null, FulfillmentMethod.DINE_IN,
                "4.25", LAST_DAY.atTime(8, 0));
        entityManager.clear();
    }

    @Test
    void statusFilterAcceptsSeveralStatusesAndSortsOldestFirst() {
        Page<OrderResponse> page = orderService.search(
                filter(List.of(OrderStatus.COMPLETED, OrderStatus.DELIVERED), null, FIRST_DAY, LAST_DAY, null, null, null),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt")));

        assertThat(ids(page)).containsExactly(lateWalkInCompleted.getId(), walkInDeliveredToContact.getId());
        assertThat(ids(orderService.search(
                filter(List.of(OrderStatus.CANCELLED), null, FIRST_DAY, LAST_DAY, null, null, null), NEWEST_FIRST)))
                .containsExactly(cancelledWalkIn.getId());
    }

    @Test
    void searchFindsAnOrderByItsNumberWithOrWithoutTheHash() {
        String number = onlineDelivery.getId().toString().substring(0, 8).toUpperCase(Locale.ROOT);

        assertThat(ids(search("#" + number))).containsExactly(onlineDelivery.getId());
        assertThat(ids(search(number.substring(0, 6).toLowerCase(Locale.ROOT)))).contains(onlineDelivery.getId());
    }

    @Test
    void searchFindsOrdersByPartOfTheCustomerOrContactName() {
        assertThat(ids(search("  sok "))).containsExactly(
                onlineDineInAwaitingPayment.getId(), onlineDelivery.getId());
        assertThat(ids(search("BOPHA"))).containsExactly(walkInDeliveredToContact.getId());
        assertThat(ids(search("100%"))).isEmpty();
    }

    @Test
    void dateRangeIncludesWholeDaysAtBothEnds() {
        Page<OrderResponse> page = orderService.search(
                filter(null, null, FIRST_DAY.plusDays(1), FIRST_DAY.plusDays(2), null, null, null), NEWEST_FIRST);

        assertThat(ids(page)).containsExactly(
                walkInDeliveredToContact.getId(), onlineDelivery.getId(), walkInPaid.getId());
    }

    @Test
    void filtersByChannelFulfillmentAndPaymentMethod() {
        assertThat(ids(orderService.search(
                filter(null, null, FIRST_DAY, LAST_DAY, OrderChannel.ONLINE, null, null), NEWEST_FIRST)))
                .containsExactly(onlineDineInAwaitingPayment.getId(), onlineDelivery.getId());
        assertThat(ids(orderService.search(
                filter(null, null, FIRST_DAY, LAST_DAY, OrderChannel.WALK_IN, FulfillmentMethod.DELIVERY, null),
                NEWEST_FIRST)))
                .containsExactly(walkInDeliveredToContact.getId());
        assertThat(ids(orderService.search(
                filter(null, null, FIRST_DAY, LAST_DAY, null, null, PaymentMethod.CASH), NEWEST_FIRST)))
                .containsExactly(walkInDeliveredToContact.getId(), walkInPaid.getId(), lateWalkInCompleted.getId());
    }

    @Test
    void pagesCountEveryMatchNotJustThePageShown() {
        Page<OrderResponse> page = orderService.search(
                filter(null, null, FIRST_DAY, LAST_DAY, null, null, null),
                PageRequest.of(1, 4, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getTotalElements()).isEqualTo(6);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(ids(page)).containsExactly(walkInPaid.getId(), lateWalkInCompleted.getId());
        assertThat(page.getContent().get(0).customerName()).isNull();
    }

    @Test
    void summaryReportsEveryStatusWithItsCountAndTotal() {
        Map<OrderStatus, OrderStatusSummaryResponse> summary = summary(
                filter(null, null, FIRST_DAY, LAST_DAY, null, null, null));

        assertThat(summary).containsOnlyKeys(OrderStatus.values());
        assertThat(summary.get(OrderStatus.PENDING).count()).isEqualTo(1);
        assertThat(summary.get(OrderStatus.PENDING).totalAmount()).isEqualByComparingTo("4.25");
        assertThat(summary.get(OrderStatus.OUT_FOR_DELIVERY).totalAmount()).isEqualByComparingTo("7.50");
        assertThat(summary.get(OrderStatus.PREPARING).count()).isZero();
        assertThat(summary.get(OrderStatus.PREPARING).totalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void summaryAppliesTheSameFiltersAsTheList() {
        Map<OrderStatus, OrderStatusSummaryResponse> summary = summary(
                filter(null, "sok", FIRST_DAY, LAST_DAY, null, null, null));

        assertThat(summary.get(OrderStatus.PENDING).count()).isEqualTo(1);
        assertThat(summary.get(OrderStatus.OUT_FOR_DELIVERY).count()).isEqualTo(1);
        assertThat(summary.values().stream().mapToLong(OrderStatusSummaryResponse::count).sum()).isEqualTo(2);
    }

    @Test
    void rejectsAStartDateAfterTheEndDate() {
        OrderSearchFilter backwards = filter(null, null, LAST_DAY, FIRST_DAY, null, null, null);

        assertThatThrownBy(() -> orderService.search(backwards, NEWEST_FIRST))
                .isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> orderService.summarizeByStatus(backwards))
                .isInstanceOf(InvalidOperationException.class);
    }

    private Page<OrderResponse> search(String text) {
        return orderService.search(filter(null, text, FIRST_DAY, LAST_DAY, null, null, null), NEWEST_FIRST);
    }

    private Map<OrderStatus, OrderStatusSummaryResponse> summary(OrderSearchFilter filter) {
        return orderService.summarizeByStatus(filter).stream()
                .collect(Collectors.toMap(OrderStatusSummaryResponse::status, row -> row));
    }

    private static OrderSearchFilter filter(List<OrderStatus> statuses, String search, LocalDate from, LocalDate to,
            OrderChannel channel, FulfillmentMethod fulfillment, PaymentMethod paymentMethod) {
        return new OrderSearchFilter(null, null, statuses, search, from, to, channel, fulfillment, paymentMethod);
    }

    private static List<UUID> ids(Page<OrderResponse> page) {
        return page.getContent().stream().map(OrderResponse::id).toList();
    }

    private Customer customer(String name) {
        Customer customer = new Customer();
        customer.setFullName(name);
        customer.setEmail(UUID.randomUUID() + "@example.test");
        customer.setPassword("unused");
        customer.setStatus(UserStatus.ACTIVE);
        customer.setRegisterType(RegisterType.EMAIL);
        return customerRepository.saveAndFlush(customer);
    }

    private Order order(Customer customer, String contactName, OrderStatus status, PaymentMethod paymentMethod,
            FulfillmentMethod fulfillment, String total, LocalDateTime placedAt) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setContactName(contactName);
        order.setStatus(status);
        order.setPaymentMethod(paymentMethod);
        order.setFulfillmentMethod(fulfillment);
        order.setTotalAmount(new BigDecimal(total));
        order = orderRepository.saveAndFlush(order);
        // createdAt is stamped by auditing when the order is saved; move it to the day the test needs.
        entityManager.createNativeQuery("update orders set created_at = ?1 where id = ?2")
                .setParameter(1, placedAt)
                .setParameter(2, order.getId())
                .executeUpdate();
        return order;
    }
}
