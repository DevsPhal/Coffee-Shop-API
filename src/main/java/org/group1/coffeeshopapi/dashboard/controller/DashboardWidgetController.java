package org.group1.coffeeshopapi.dashboard.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.dashboard.dto.request.DashboardWidgetRequest;
import org.group1.coffeeshopapi.dashboard.dto.response.DashboardWidgetResponse;
import org.group1.coffeeshopapi.dashboard.service.DashboardWidgetService;
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
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/dashboard/widgets")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DashboardWidgetController {

    private final DashboardWidgetService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DashboardWidgetResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<DashboardWidgetResponse>>builder().status(HttpStatus.OK.value()).message("Widgets retrieved").data(service.getAll()).timeStamp(LocalDateTime.now()).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DashboardWidgetResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.<DashboardWidgetResponse>builder().status(HttpStatus.OK.value()).message("Widget retrieved").data(service.getById(id)).timeStamp(LocalDateTime.now()).build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DashboardWidgetResponse>> create(@RequestBody DashboardWidgetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<DashboardWidgetResponse>builder().status(HttpStatus.CREATED.value()).message("Widget created").data(service.create(request)).timeStamp(LocalDateTime.now()).build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DashboardWidgetResponse>> update(@PathVariable Long id, @RequestBody DashboardWidgetRequest request) {
        return ResponseEntity.ok(ApiResponse.<DashboardWidgetResponse>builder().status(HttpStatus.OK.value()).message("Widget updated").data(service.update(id, request)).timeStamp(LocalDateTime.now()).build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.<Void>builder().status(HttpStatus.NO_CONTENT.value()).message("Widget deleted").timeStamp(LocalDateTime.now()).build());
    }
}
