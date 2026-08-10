package org.group1.coffeeshopapi.audittrail.mapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.group1.coffeeshopapi.audittrail.dto.request.AuditLogCreateRequest;
import org.group1.coffeeshopapi.audittrail.dto.response.AuditLogResponse;
import org.group1.coffeeshopapi.audittrail.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {
	@Mapping(target = "timestamp", expression = "java(formatTimestamp(auditLog.getCreatedAt()))")
	@Mapping(target = "actor", source = "actorName")
	AuditLogResponse toResponse(AuditLog auditLog);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "auditId", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	AuditLog toEntity(AuditLogCreateRequest request);

	default String formatTimestamp(LocalDateTime dateTime) {
		if (dateTime == null) {
			return null;
		}
		return dateTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss"));
	}
}