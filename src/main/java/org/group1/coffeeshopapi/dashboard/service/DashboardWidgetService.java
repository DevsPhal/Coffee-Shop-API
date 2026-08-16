package org.group1.coffeeshopapi.dashboard.service;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.dashboard.dto.request.DashboardWidgetRequest;
import org.group1.coffeeshopapi.dashboard.dto.response.DashboardWidgetResponse;

public interface DashboardWidgetService {
    DashboardWidgetResponse create(DashboardWidgetRequest request);
    DashboardWidgetResponse getById(UUID id);
    DashboardWidgetResponse update(UUID id, DashboardWidgetRequest request);
    void delete(UUID id);
    List<DashboardWidgetResponse> getAll();
}
