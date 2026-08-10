package org.group1.coffeeshopapi.dashboard.mapper;

import org.group1.coffeeshopapi.dashboard.dto.request.DashboardWidgetRequest;
import org.group1.coffeeshopapi.dashboard.dto.response.DashboardWidgetResponse;
import org.group1.coffeeshopapi.dashboard.entity.DashboardWidget;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DashboardWidgetMapper {
    DashboardWidget toEntity(DashboardWidgetRequest request);

    DashboardWidgetResponse toResponse(DashboardWidget widget);

    void updateEntity(DashboardWidgetRequest request, @MappingTarget DashboardWidget widget);
}
