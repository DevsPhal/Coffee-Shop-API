package org.group1.coffeeshopapi.report.controller;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.report.dto.request.ReportRequest;
import org.group1.coffeeshopapi.report.dto.response.ReportResponse;
import org.group1.coffeeshopapi.report.dto.response.SalesReportResponse;
import org.group1.coffeeshopapi.report.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(@RequestBody ReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<ReportResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Report created successfully")
                .data(reportService.createReport(request))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getAllReports() {
        return ResponseEntity.ok(ApiResponse.<List<ReportResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Reports retrieved successfully")
                .data(reportService.getAllReports())
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<ReportResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Report retrieved successfully")
                .data(reportService.getReportById(id))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> updateReport(@PathVariable UUID id, @RequestBody ReportRequest request) {
        return ResponseEntity.ok(ApiResponse.<ReportResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Report updated successfully")
                .data(reportService.updateReport(id, request))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable UUID id) {
        reportService.deleteReport(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.<Void>builder()
                .status(HttpStatus.NO_CONTENT.value())
                .message("Report deleted successfully")
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<SalesReportResponse>> sales(@RequestParam String start, @RequestParam String end) {
        return ResponseEntity.ok(ApiResponse.<SalesReportResponse>builder().status(HttpStatus.OK.value()).message("Sales report retrieved successfully").data(reportService.sales(start, end)).timeStamp(LocalDateTime.now()).build());
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<SalesReportResponse>> orders(@RequestParam String start, @RequestParam String end) {
        return ResponseEntity.ok(ApiResponse.<SalesReportResponse>builder().status(HttpStatus.OK.value()).message("Orders report retrieved successfully").data(reportService.orders(start, end)).timeStamp(LocalDateTime.now()).build());
    }

    @GetMapping("/inventory")
    public ResponseEntity<ApiResponse<SalesReportResponse>> inventory() {
        return ResponseEntity.ok(ApiResponse.<SalesReportResponse>builder().status(HttpStatus.OK.value()).message("Inventory report retrieved successfully").data(reportService.inventory()).timeStamp(LocalDateTime.now()).build());
    }

    @GetMapping("/export")
    public ResponseEntity<ApiResponse<String>> export(@RequestParam String format) {
        return ResponseEntity.ok(ApiResponse.<String>builder().status(HttpStatus.OK.value()).message("Report exported successfully").data(reportService.export(format)).timeStamp(LocalDateTime.now()).build());
    }
}
