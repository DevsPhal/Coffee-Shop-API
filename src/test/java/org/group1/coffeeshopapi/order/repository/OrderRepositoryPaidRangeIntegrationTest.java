package org.group1.coffeeshopapi.order.repository;

import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.order.entity.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Real queries against H2: a sale paid exactly at midnight belongs to the new day only.
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderRepositoryPaidRangeIntegrationTest {

    @Autowired private OrderRepository orderRepository;

    private final LocalDate day = LocalDate.of(2026, 3, 10);
    private final UUID barista = UUID.randomUUID();

    @Test
    void aSaleAtMidnightIsCountedOnExactlyOneDay() {
        Order lateSale = paidAt(day.atTime(23, 59, 59));
        Order midnightSale = paidAt(day.plusDays(1).atStartOfDay());
        orderRepository.saveAllAndFlush(List.of(lateSale, midnightSale));

        List<Order> firstDay = orderRepository.findPaidInRange(day.atStartOfDay(), day.plusDays(1).atStartOfDay());
        List<Order> secondDay = orderRepository.findPaidInRange(day.plusDays(1).atStartOfDay(), day.plusDays(2).atStartOfDay());

        assertThat(firstDay).extracting(Order::getId).containsExactly(lateSale.getId());
        assertThat(secondDay).extracting(Order::getId).containsExactly(midnightSale.getId());
    }

    @Test
    void theBaristaReportUsesTheSameBoundary() {
        Order midnightSale = paidAt(day.plusDays(1).atStartOfDay());
        orderRepository.saveAndFlush(midnightSale);

        assertThat(orderRepository.findPaidByHandledByInRange(
                barista, day.atStartOfDay(), day.plusDays(1).atStartOfDay())).isEmpty();
        assertThat(orderRepository.findPaidByHandledByInRange(
                barista, day.plusDays(1).atStartOfDay(), day.plusDays(2).atStartOfDay())).hasSize(1);
    }

    private Order paidAt(LocalDateTime paidAt) {
        Order order = new Order();
        order.setStatus(OrderStatus.COMPLETED);
        order.setPaymentMethod(PaymentMethod.CASH);
        order.setTotalAmount(new BigDecimal("2.00"));
        order.setHandledBy(barista);
        order.setPaidAt(paidAt);
        return order;
    }
}
