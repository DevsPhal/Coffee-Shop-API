package org.group1.coffeeshopapi.admin.controller;

import org.group1.coffeeshopapi.admin.dto.response.AdminDashboardResponse;
import org.group1.coffeeshopapi.admin.service.AdminService;
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
	public AdminDashboardResponse getDashboardSummary() {
		return adminService.getDashboardSummary();
	}
}
