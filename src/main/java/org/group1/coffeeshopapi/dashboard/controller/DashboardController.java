package org.group1.coffeeshopapi.dashboard.controller;

import org.group1.coffeeshopapi.dashboard.dto.response.DashboardSummaryResponse;
import org.group1.coffeeshopapi.dashboard.service.Dashboard;
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
    public DashboardSummaryResponse getSummary() {
        return dashboard.getSummary();
    }
}
