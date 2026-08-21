package org.group1.coffeeshopapi.order.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.order.dto.response.AdminDailyReportResponse;
import org.group1.coffeeshopapi.order.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Reports", description = "Admin only: view every barista's daily sales report")
@SecurityRequirement(name = "bearerAuth")
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping("/daily")
    public ApiResponse<AdminDailyReportResponse> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate reportDate = date != null ? date : LocalDate.now();
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, reportService.getDailyReport(reportDate));
    }
}
