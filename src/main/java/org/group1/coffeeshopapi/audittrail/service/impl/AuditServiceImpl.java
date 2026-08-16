package org.group1.coffeeshopapi.audittrail.service.impl;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.Optional;

import org.group1.coffeeshopapi.audittrail.dto.request.AuditLogCreateRequest;
import org.group1.coffeeshopapi.audittrail.dto.response.AuditLogResponse;
import org.group1.coffeeshopapi.audittrail.entity.AuditLog;
import org.group1.coffeeshopapi.audittrail.mapper.AuditLogMapper;
import org.group1.coffeeshopapi.audittrail.repository.AuditLogRepository;
import org.group1.coffeeshopapi.audittrail.service.AuditService;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class AuditServiceImpl implements AuditService {
	private final AuditLogRepository auditLogRepository;
	private final AuditLogMapper auditLogMapper;

	@Override
	@Transactional
	public void log(String actorId,
					String actorName,
					String role,
					String entity,
					String entityId,
					String action,
					String description,
					String ipAddress) {
		AuditLog entry = AuditLog.builder()
				.actorId(actorId)
				.actorName(actorName)
				.role(role)
				.entity(entity)
				.entityId(entityId)
				.action(action)
				.description(description)
				.ipAddress(ipAddress)
				.build();
		auditLogRepository.save(entry);
	}

	@Override
	@Transactional
	public AuditLogResponse create(AuditLogCreateRequest request) {
		AuditLog saved = auditLogRepository.save(auditLogMapper.toEntity(request));
		return auditLogMapper.toResponse(saved);
	}

	@Override
	public Page<AuditLogResponse> getAll(Pageable pageable) {
		return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(auditLogMapper::toResponse);
	}

	@Override
	public Page<AuditLogResponse> query(String actorId, String entity, LocalDateTime from, LocalDateTime to, Pageable pageable) {
		return auditLogRepository.queryWithFilters(actorId, entity, from, to, pageable).map(auditLogMapper::toResponse);
	}

	@Override
	public Optional<AuditLogResponse> getById(UUID id) {
		return auditLogRepository.findById(id).map(auditLogMapper::toResponse);
	}

	@Override
	public long getTotalCount() {
		return auditLogRepository.count();
	}

	@Override
	@Transactional
	public void delete(UUID id) {
		if (!auditLogRepository.existsById(id)) {
			throw new ResourceNotFoundException("Audit log not found: " + id);
		}
		auditLogRepository.deleteById(id);
	}
}
