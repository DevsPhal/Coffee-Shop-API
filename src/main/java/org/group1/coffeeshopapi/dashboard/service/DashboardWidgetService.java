package org.group1.coffeeshopapi.dashboard.service;

import java.util.List;

import org.group1.coffeeshopapi.dashboard.dto.request.DashboardWidgetRequest;
import org.group1.coffeeshopapi.dashboard.dto.response.DashboardWidgetResponse;

public interface DashboardWidgetService {
    DashboardWidgetResponse create(DashboardWidgetRequest request);
    DashboardWidgetResponse getById(Long id);
    DashboardWidgetResponse update(Long id, DashboardWidgetRequest request);
    void delete(Long id);
    List<DashboardWidgetResponse> getAll();
}
