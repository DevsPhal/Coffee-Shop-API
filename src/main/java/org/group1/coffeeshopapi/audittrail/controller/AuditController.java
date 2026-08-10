package org.group1.coffeeshopapi.audittrail.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.audittrail.dto.request.AuditLogCreateRequest;
import org.group1.coffeeshopapi.audittrail.dto.response.AuditLogResponse;
import org.group1.coffeeshopapi.audittrail.service.AuditService;
import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {

	private final AuditService auditService;

	@GetMapping
	public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getAll(
			@RequestParam(required = false) String actor,
			@RequestParam(required = false) String entity,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
			@PageableDefault(size = 20) Pageable pageable) {
		List<AuditLogResponse> logs = (actor != null || entity != null || from != null || to != null)
				? auditService.query(actor, entity, from, to, pageable).getContent()
				: auditService.getAll(pageable).getContent();
		return ResponseEntity.ok(ApiResponse.<List<AuditLogResponse>>builder()
				.status(HttpStatus.OK.value())
				.message("Audit logs retrieved successfully")
				.data(logs)
				.timeStamp(LocalDateTime.now())
				.build());
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<AuditLogResponse>> getById(@PathVariable Long id) {
		return auditService.getById(id)
				.map(response -> ResponseEntity.ok(ApiResponse.<AuditLogResponse>builder()
						.status(HttpStatus.OK.value())
						.message("Audit log retrieved successfully")
						.data(response)
						.timeStamp(LocalDateTime.now())
						.build()))
				.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(ApiResponse.<AuditLogResponse>builder()
							.status(HttpStatus.NOT_FOUND.value())
							.message("Audit log not found")
							.timeStamp(LocalDateTime.now())
							.build()));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<AuditLogResponse>> create(@RequestBody AuditLogCreateRequest request) {
		AuditLogResponse created = auditService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.<AuditLogResponse>builder()
						.status(HttpStatus.CREATED.value())
						.message("Audit log created successfully")
						.data(created)
						.timeStamp(LocalDateTime.now())
						.build());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
		auditService.delete(id);
		return ResponseEntity.ok(ApiResponse.<Void>builder()
				.status(HttpStatus.OK.value())
				.message("Audit log deleted successfully")
				.timeStamp(LocalDateTime.now())
				.build());
	}

	@GetMapping("/count")
	public ResponseEntity<ApiResponse<Long>> count() {
		return ResponseEntity.ok(ApiResponse.<Long>builder()
				.status(HttpStatus.OK.value())
				.message("Audit log count retrieved successfully")
				.data(auditService.getTotalCount())
				.timeStamp(LocalDateTime.now())
				.build());
	}
}
