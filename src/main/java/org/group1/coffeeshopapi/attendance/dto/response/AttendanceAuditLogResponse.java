package org.group1.coffeeshopapi.attendance.dto.response;

import org.group1.coffeeshopapi.common.enums.AttendanceAuditAction;
import org.group1.coffeeshopapi.common.enums.Role;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttendanceAuditLogResponse(
        UUID id,
        UUID attendanceId,
        AttendanceAuditAction action,
        UUID actorId,
        String actorName,
        Role actorRole,
        String note,
        LocalDateTime createdAt
) {
}
