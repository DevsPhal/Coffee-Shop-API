package org.group1.coffeeshopapi.finance.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.finance.dto.response.FinanceSummaryResponse;
import org.group1.coffeeshopapi.finance.service.FinanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
@Tag(name = "Admin Finance", description = "Admin only: money in/out and profit, by day/month/year")
@SecurityRequirement(name = "bearerAuth")
public class AdminFinanceController {

    private final FinanceService financeService;

    @GetMapping("/daily")
    public ApiResponse<FinanceSummaryResponse> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, financeService.getDaily(target));
    }

    @GetMapping("/monthly")
    public ApiResponse<FinanceSummaryResponse> monthly(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        LocalDate now = LocalDate.now();
        int targetYear = year != null ? year : now.getYear();
        int targetMonth = month != null ? month : now.getMonthValue();
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, financeService.getMonthly(targetYear, targetMonth));
    }

    @GetMapping("/yearly")
    public ApiResponse<FinanceSummaryResponse> yearly(@RequestParam(required = false) Integer year) {
        int targetYear = year != null ? year : LocalDate.now().getYear();
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, financeService.getYearly(targetYear));
    }
}
