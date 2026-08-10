package org.group1.coffeeshopapi.audittrail.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private UUID id;
    private String auditId;
    private String timestamp;
    private String actorId;
    private String actor;
    private String role;
    private String entity;
    private String entityId;
    private String action;
    private String description;
    private String ipAddress;
}