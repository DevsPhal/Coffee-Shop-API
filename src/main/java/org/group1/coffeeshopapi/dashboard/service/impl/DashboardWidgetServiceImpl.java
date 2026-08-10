package org.group1.coffeeshopapi.dashboard.service.impl;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.dashboard.dto.request.DashboardWidgetRequest;
import org.group1.coffeeshopapi.dashboard.dto.response.DashboardWidgetResponse;
import org.group1.coffeeshopapi.dashboard.entity.DashboardWidget;
import org.group1.coffeeshopapi.dashboard.mapper.DashboardWidgetMapper;
import org.group1.coffeeshopapi.dashboard.repository.DashboardWidgetRepository;
import org.group1.coffeeshopapi.dashboard.service.DashboardWidgetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DashboardWidgetServiceImpl implements DashboardWidgetService {

    private final DashboardWidgetRepository repository;
    private final DashboardWidgetMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<DashboardWidgetResponse> getAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardWidgetResponse getById(UUID id) {
        DashboardWidget w = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Widget not found: " + id));
        return mapper.toResponse(w);
    }

    @Override
    public DashboardWidgetResponse create(DashboardWidgetRequest request) {
        DashboardWidget w = mapper.toEntity(request);
        DashboardWidget saved = repository.save(w);
        return mapper.toResponse(saved);
    }

    @Override
    public DashboardWidgetResponse update(UUID id, DashboardWidgetRequest request) {
        DashboardWidget w = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Widget not found: " + id));
        mapper.updateEntity(request, w);
        return mapper.toResponse(repository.save(w));
    }

    @Override
    public void delete(UUID id) {
        DashboardWidget w = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Widget not found: " + id));
        repository.delete(w);
    }
}
