package org.group1.coffeeshopapi.audittrail.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogCreateRequest {
	private String actorId;
	private String actorName;
	private String role;
	private String entity;
	private String entityId;
	private String action;
	private String description;
	private String ipAddress;
}