package org.group1.coffeeshopapi.audittrail.service;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.Optional;

import org.group1.coffeeshopapi.audittrail.dto.request.AuditLogCreateRequest;
import org.group1.coffeeshopapi.audittrail.dto.response.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditService {

	void log(String actorId, String actorName, String role, String entity, String entityId, String action, String description,
			String ipAddress);

	AuditLogResponse create(AuditLogCreateRequest request);

	Page<AuditLogResponse> getAll(Pageable pageable);

	Page<AuditLogResponse> getByActor(String actorId, Pageable pageable);

	Page<AuditLogResponse> getByEntity(String entity, Pageable pageable);

	Page<AuditLogResponse> query(String actorId, String entity, LocalDateTime from, LocalDateTime to, Pageable pageable);

	Optional<AuditLogResponse> getById(UUID id);

	long getTotalCount();

	void delete(UUID id);
}
