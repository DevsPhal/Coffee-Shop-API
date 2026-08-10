package org.group1.coffeeshopapi.report.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.group1.coffeeshopapi.common.exception.InvalidRequestException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.orders.entity.Order;
import org.group1.coffeeshopapi.orders.repository.OrderRepository;
import org.group1.coffeeshopapi.payments.repository.PaymentRepository;
import org.group1.coffeeshopapi.report.dto.request.ReportRequest;
import org.group1.coffeeshopapi.report.dto.response.ReportResponse;
import org.group1.coffeeshopapi.report.dto.response.SalesReportResponse;
import org.group1.coffeeshopapi.report.entity.Report;
import org.group1.coffeeshopapi.report.mapper.ReportMapper;
import org.group1.coffeeshopapi.report.repository.ReportRepository;
import org.group1.coffeeshopapi.report.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReportImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public ReportResponse createReport(ReportRequest request) {
        Report report = reportMapper.toEntity(request);
        Report saved = reportRepository.save(report);
        return reportMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponse getReportById(Long id) {
        return reportRepository.findById(id)
                .map(reportMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
    }

    @Override
    @Transactional
    public ReportResponse updateReport(Long id, ReportRequest request) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
        reportMapper.updateEntity(request, report);
        return reportMapper.toResponse(reportRepository.save(report));
    }

    @Override
    @Transactional
    public void deleteReport(Long id) {
        if (!reportRepository.existsById(id)) {
            throw new ResourceNotFoundException("Report not found: " + id);
        }
        reportRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports() {
        return reportRepository.findAll().stream().map(reportMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SalesReportResponse sales(String start, String end) {
        LocalDateTime rangeStart = parseStartOfDay(start);
        LocalDateTime rangeEnd = parseEndOfDay(end);

        List<Order> orders = orderRepository.findByCreatedAtBetween(rangeStart, rangeEnd);
        double totalSales = orders.stream().mapToDouble(Order::getTotalAmount).sum();
        long totalOrders = orders.size();
        double avg = totalOrders == 0 ? 0 : totalSales / totalOrders;

        SalesReportResponse resp = new SalesReportResponse();
        resp.setPeriod(start + " to " + end);
        resp.setTotalSales(totalSales);
        resp.setTotalOrders(totalOrders);
        resp.setAverageOrderValue(avg);
        resp.setTopProducts(topProductsFromOrders(orders));
        resp.setPaymentMethods(paymentMethods());
        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public SalesReportResponse orders(String start, String end) {
        // Same underlying order data as sales(), just semantically the "orders" view for now.
        return sales(start, end);
    }

    @Override
    public SalesReportResponse inventory() {
        SalesReportResponse resp = new SalesReportResponse();
        resp.setPeriod("inventory");
        resp.setTotalSales(0);
        resp.setTotalOrders(0);
        resp.setAverageOrderValue(0);
        resp.setTopProducts(List.of());
        resp.setPaymentMethods(List.of());
        return resp;
    }

    @Override
    public String export(String format) {
        if (format == null || format.isBlank()) {
            throw new InvalidRequestException("Export format is required");
        }
        return "EXPORT_" + format.toUpperCase();
    }

    private LocalDateTime parseStartOfDay(String date) {
        return parseDate(date).atStartOfDay();
    }

    private LocalDateTime parseEndOfDay(String date) {
        return parseDate(date).atTime(23, 59, 59);
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new InvalidRequestException("Invalid date: " + date + ". Expected format: yyyy-MM-dd");
        }
    }

    private List<SalesReportResponse.KeyCount> topProductsFromOrders(List<Order> orders) {
        Map<String, Integer> counts = orders.stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(item -> item.getProductName(), Collectors.summingInt(i -> i.getQuantity())));

        return counts.entrySet().stream().map(e -> {
            SalesReportResponse.KeyCount kc = new SalesReportResponse.KeyCount();
            kc.setName(e.getKey());
            kc.setCount(e.getValue());
            return kc;
        }).collect(Collectors.toList());
    }

    private List<SalesReportResponse.MethodAmount> paymentMethods() {
        var payments = paymentRepository.findAll();
        Map<String, Double> totals = payments.stream().collect(Collectors.groupingBy(p -> p.getMethod().name(), Collectors.summingDouble(p -> p.getAmount())));
        return totals.entrySet().stream().map(e -> {
            SalesReportResponse.MethodAmount m = new SalesReportResponse.MethodAmount();
            m.setMethod(e.getKey());
            m.setAmount(e.getValue());
            return m;
        }).collect(Collectors.toList());
    }

}
