package org.group1.coffeeshopapi.attendance.repository;

import org.group1.coffeeshopapi.attendance.entity.AttendanceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttendanceAuditLogRepository extends JpaRepository<AttendanceAuditLog, UUID> {
    List<AttendanceAuditLog> findByAttendanceIdOrderByCreatedAtAsc(UUID attendanceId);
}
