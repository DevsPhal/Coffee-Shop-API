package org.group1.coffeeshopapi.order.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.order.dto.response.AdminDailyReportResponse;
import org.group1.coffeeshopapi.order.dto.response.DailyReportResponse;
import org.group1.coffeeshopapi.order.entity.Order;
import org.group1.coffeeshopapi.order.repository.OrderRepository;
import org.group1.coffeeshopapi.order.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final BaristaRepository baristaRepository;

    @Override
    public DailyReportResponse getOwnDailyReport(UUID baristaId, LocalDate date) {
        List<Order> orders = orderRepository.findByBaristaIdAndStatusAndPaidAtBetween(
                baristaId, OrderStatus.COMPLETED, startOfDay(date), endOfDay(date));
        String baristaName = baristaRepository.findById(baristaId).map(Barista::getFullName).orElse(null);
        return summarize(baristaId, baristaName, date, orders);
    }

    @Override
    public AdminDailyReportResponse getDailyReport(LocalDate date) {
        List<Order> orders = orderRepository.findByStatusAndPaidAtBetween(
                OrderStatus.COMPLETED, startOfDay(date), endOfDay(date));

        // Self-service customer orders paid via Bakong have no barista attached — they still
        // count toward shop-wide totals below, but there's no one to attribute a report row to.
        List<Order> baristaHandled = orders.stream().filter(order -> order.getBaristaId() != null).toList();

        Map<UUID, String> baristaNames = baristaRepository.findAllById(
                        baristaHandled.stream().map(Order::getBaristaId).distinct().toList()).stream()
                .collect(Collectors.toMap(Barista::getId, Barista::getFullName));

        Map<UUID, List<Order>> byBarista = baristaHandled.stream().collect(Collectors.groupingBy(Order::getBaristaId));
        List<DailyReportResponse> baristaReports = byBarista.entrySet().stream()
                .map(entry -> summarize(entry.getKey(), baristaNames.get(entry.getKey()), date, entry.getValue()))
                .sorted((a, b) -> b.grandTotal().compareTo(a.grandTotal()))
                .toList();

        BigDecimal cashTotal = sumByMethod(orders, PaymentMethod.CASH);
        BigDecimal bakongTotal = sumByMethod(orders, PaymentMethod.BAKONG);

        return new AdminDailyReportResponse(
                date, orders.size(), cashTotal, bakongTotal, cashTotal.add(bakongTotal), baristaReports);
    }

    private DailyReportResponse summarize(UUID baristaId, String baristaName, LocalDate date, List<Order> orders) {
        BigDecimal cashTotal = sumByMethod(orders, PaymentMethod.CASH);
        BigDecimal bakongTotal = sumByMethod(orders, PaymentMethod.BAKONG);
        return new DailyReportResponse(
                baristaId, baristaName, date, orders.size(), cashTotal, bakongTotal, cashTotal.add(bakongTotal));
    }

    private BigDecimal sumByMethod(List<Order> orders, PaymentMethod method) {
        return orders.stream()
                .filter(order -> order.getPaymentMethod() == method)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay();
    }
}
