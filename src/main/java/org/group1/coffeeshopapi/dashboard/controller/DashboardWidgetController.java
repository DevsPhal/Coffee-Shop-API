package org.group1.coffeeshopapi.dashboard.controller;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.dashboard.dto.request.DashboardWidgetRequest;
import org.group1.coffeeshopapi.dashboard.dto.response.DashboardWidgetResponse;
import org.group1.coffeeshopapi.dashboard.service.DashboardWidgetService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/dashboard/widgets")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class DashboardWidgetController {

    private final DashboardWidgetService service;

    @GetMapping
    public List<DashboardWidgetResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public DashboardWidgetResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public DashboardWidgetResponse create(@RequestBody DashboardWidgetRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public DashboardWidgetResponse update(@PathVariable UUID id, @RequestBody DashboardWidgetRequest request) {
        return service.update(id, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
