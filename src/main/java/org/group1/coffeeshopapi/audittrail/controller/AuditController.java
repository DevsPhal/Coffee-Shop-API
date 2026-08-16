package org.group1.coffeeshopapi.audittrail.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.audittrail.dto.request.AuditLogCreateRequest;
import org.group1.coffeeshopapi.audittrail.dto.response.AuditLogResponse;
import org.group1.coffeeshopapi.audittrail.service.AuditService;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {

	private final AuditService auditService;

	@GetMapping
	public List<AuditLogResponse> getAll(
			@RequestParam(required = false) String actor,
			@RequestParam(required = false) String entity,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
			@PageableDefault(size = 20) Pageable pageable) {
		return (actor != null || entity != null || from != null || to != null)
				? auditService.query(actor, entity, from, to, pageable).getContent()
				: auditService.getAll(pageable).getContent();
	}

	@GetMapping("/{id}")
	public AuditLogResponse getById(@PathVariable UUID id) {
		return auditService.getById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Audit log not found: " + id));
	}

	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public AuditLogResponse create(@RequestBody AuditLogCreateRequest request) {
		return auditService.create(request);
	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/{id}")
	public void delete(@PathVariable UUID id) {
		auditService.delete(id);
	}

	@GetMapping("/count")
	public long count() {
		return auditService.getTotalCount();
	}
}
