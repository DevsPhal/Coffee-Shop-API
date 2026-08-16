package org.group1.coffeeshopapi.report.controller;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.report.dto.request.ReportRequest;
import org.group1.coffeeshopapi.report.dto.response.ReportResponse;
import org.group1.coffeeshopapi.report.dto.response.SalesReportResponse;
import org.group1.coffeeshopapi.report.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ReportResponse createReport(@RequestBody ReportRequest request) {
        return reportService.createReport(request);
    }

    @GetMapping
    public List<ReportResponse> getAllReports() {
        return reportService.getAllReports();
    }

    @GetMapping("/{id}")
    public ReportResponse getReportById(@PathVariable UUID id) {
        return reportService.getReportById(id);
    }

    @PutMapping("/{id}")
    public ReportResponse updateReport(@PathVariable UUID id, @RequestBody ReportRequest request) {
        return reportService.updateReport(id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void deleteReport(@PathVariable UUID id) {
        reportService.deleteReport(id);
    }

    @GetMapping("/sales")
    public SalesReportResponse sales(@RequestParam String start, @RequestParam String end) {
        return reportService.sales(start, end);
    }

    @GetMapping("/orders")
    public SalesReportResponse orders(@RequestParam String start, @RequestParam String end) {
        return reportService.orders(start, end);
    }

    @GetMapping("/inventory")
    public SalesReportResponse inventory() {
        return reportService.inventory();
    }

    @GetMapping("/export")
    public String export(@RequestParam String format) {
        return reportService.export(format);
    }
}
