package org.group1.coffeeshopapi.attendance.service;

import org.group1.coffeeshopapi.attendance.dto.request.CreateAttendanceRequest;
import org.group1.coffeeshopapi.attendance.dto.request.UpdateAttendanceRequest;
import org.group1.coffeeshopapi.attendance.dto.response.AttendanceAuditLogResponse;
import org.group1.coffeeshopapi.attendance.dto.response.AttendanceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AttendanceService {

    // ---------- Barista self-service ----------
    AttendanceResponse checkIn(UUID baristaId);
    AttendanceResponse checkOut(UUID baristaId);
    AttendanceResponse getCurrentOpenShift(UUID baristaId);
    Page<AttendanceResponse> listOwn(UUID baristaId, Pageable pageable);

    // ---------- Admin ----------
    Page<AttendanceResponse> listAll(UUID baristaId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    AttendanceResponse getById(UUID id);
    AttendanceResponse create(CreateAttendanceRequest request, UUID actorId);
    AttendanceResponse update(UUID id, UpdateAttendanceRequest request, UUID actorId);
    List<AttendanceAuditLogResponse> getHistory(UUID id);
}
