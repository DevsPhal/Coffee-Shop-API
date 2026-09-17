package org.group1.coffeeshopapi.order.service.impl;

import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.Role;
import org.group1.coffeeshopapi.order.dto.response.AdminDailyReportResponse;
import org.group1.coffeeshopapi.order.dto.response.DailyReportResponse;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// Covers the daily takings report: per-barista grouping, shop-wide totals including
// no-staff-attached Bakong sales, and the highest-earner-first ordering.
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ActorLookupService actorLookupService;
    @InjectMocks private ReportServiceImpl service;

    @Test
    void ownDailyReportSumsCashAndBakongSeparatelyForOneBarista() {
        UUID baristaId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 3, 10);
        when(orderRepository.findByHandledByAndPaidAtBetween(any(), any(), any())).thenReturn(List.of(
                order(baristaId, PaymentMethod.CASH, "10.00"),
                order(baristaId, PaymentMethod.BAKONG, "7.50")));
        when(actorLookupService.resolve(baristaId)).thenReturn(new ActorSummary(baristaId, "Sophal", Role.BARISTA));

        DailyReportResponse report = service.getOwnDailyReport(baristaId, date);

        assertThat(report.baristaName()).isEqualTo("Sophal");
        assertThat(report.totalOrders()).isEqualTo(2);
        assertThat(report.cashTotal()).isEqualByComparingTo("10.00");
        assertThat(report.bakongTotal()).isEqualByComparingTo("7.50");
        assertThat(report.grandTotal()).isEqualByComparingTo("17.50");
    }

    @Test
    void adminDailyReportCountsUnattachedBakongSalesInTheShopTotalButNotInAnyBaristaRow() {
        LocalDate date = LocalDate.of(2026, 3, 10);
        UUID barista = UUID.randomUUID();
        Order selfServiceBakongSale = order(null, PaymentMethod.BAKONG, "9.00");

        when(orderRepository.findByPaidAtBetween(any(), any())).thenReturn(List.of(
                order(barista, PaymentMethod.CASH, "20.00"),
                selfServiceBakongSale));
        when(actorLookupService.resolveAll(any())).thenReturn(Map.of(barista, new ActorSummary(barista, "Dara", Role.BARISTA)));

        AdminDailyReportResponse report = service.getDailyReport(date);

        assertThat(report.totalOrders()).isEqualTo(2);
        assertThat(report.cashTotal()).isEqualByComparingTo("20.00");
        assertThat(report.bakongTotal()).isEqualByComparingTo("9.00");
        assertThat(report.grandTotal()).isEqualByComparingTo("29.00");
        assertThat(report.baristas()).hasSize(1);
        assertThat(report.baristas().get(0).baristaId()).isEqualTo(barista);
        assertThat(report.baristas().get(0).grandTotal()).isEqualByComparingTo("20.00");
    }

    @Test
    void adminDailyReportOrdersBaristasByGrandTotalHighestFirst() {
        LocalDate date = LocalDate.of(2026, 3, 10);
        UUID topEarner = UUID.randomUUID();
        UUID lowerEarner = UUID.randomUUID();

        when(orderRepository.findByPaidAtBetween(any(), any())).thenReturn(List.of(
                order(lowerEarner, PaymentMethod.CASH, "5.00"),
                order(topEarner, PaymentMethod.CASH, "50.00")));
        when(actorLookupService.resolveAll(any())).thenReturn(Map.of(
                topEarner, new ActorSummary(topEarner, "Top", Role.BARISTA),
                lowerEarner, new ActorSummary(lowerEarner, "Lower", Role.BARISTA)));

        AdminDailyReportResponse report = service.getDailyReport(date);

        assertThat(report.baristas()).extracting(DailyReportResponse::baristaId)
                .containsExactly(topEarner, lowerEarner);
    }

    private Order order(UUID handledBy, PaymentMethod method, String amount) {
        Order order = new Order();
        order.setHandledBy(handledBy);
        order.setPaymentMethod(method);
        order.setTotalAmount(new BigDecimal(amount));
        return order;
    }
}
