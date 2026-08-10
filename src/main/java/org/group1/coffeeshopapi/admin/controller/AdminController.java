package org.group1.coffeeshopapi.admin.controller;

import java.time.LocalDateTime;

import org.group1.coffeeshopapi.admin.dto.response.AdminDashboardResponse;
import org.group1.coffeeshopapi.admin.service.AdminService;
import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/v1/admin"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

	private final AdminService adminService;

	@GetMapping("/summary")
	public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboardSummary() {
		AdminDashboardResponse summary = adminService.getDashboardSummary();
		return ResponseEntity.ok(ApiResponse.<AdminDashboardResponse>builder()
				.status(HttpStatus.OK.value())
				.message("Admin dashboard summary retrieved successfully")
				.data(summary)
				.timeStamp(LocalDateTime.now())
				.build());
	}
}
