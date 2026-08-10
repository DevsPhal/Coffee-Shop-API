package org.group1.coffeeshopapi.dashboard.controller;

import java.time.LocalDateTime;

import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.dashboard.dto.response.DashboardSummaryResponse;
import org.group1.coffeeshopapi.dashboard.service.Dashboard;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DashboardController {

    private final Dashboard dashboard;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        DashboardSummaryResponse summary = dashboard.getSummary();
        return ResponseEntity.ok(ApiResponse.<DashboardSummaryResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Dashboard summary retrieved successfully")
                .data(summary)
                .timeStamp(LocalDateTime.now())
                .build());
    }
}