package org.group1.coffeeshopapi.report.mapper;

import org.group1.coffeeshopapi.report.dto.request.ReportRequest;
import org.group1.coffeeshopapi.report.dto.response.ReportResponse;
import org.group1.coffeeshopapi.report.entity.Report;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ReportMapper {

    Report toEntity(ReportRequest request);

    ReportResponse toResponse(Report report);

    void updateEntity(ReportRequest request, @MappingTarget Report report);
}
