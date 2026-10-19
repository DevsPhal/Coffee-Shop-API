package org.group1.coffeeshopapi.attendance.service;

import org.group1.coffeeshopapi.attendance.dto.request.AdminCheckInRequest;
import org.group1.coffeeshopapi.attendance.dto.request.AdminCheckOutRequest;
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

    AttendanceResponse checkIn(UUID baristaId, String note);
    AttendanceResponse checkOut(UUID baristaId, String note);
    AttendanceResponse getCurrentOpenShift(UUID baristaId);
    Page<AttendanceResponse> listOwn(UUID baristaId, Pageable pageable);

    Page<AttendanceResponse> listAll(UUID baristaId, LocalDateTime from, LocalDateTime to, Boolean open, Pageable pageable);
    AttendanceResponse getById(UUID id);
    AttendanceResponse adminCheckIn(AdminCheckInRequest request, UUID actorId);
    AttendanceResponse adminCheckOut(AdminCheckOutRequest request, UUID actorId);
    AttendanceResponse create(CreateAttendanceRequest request, UUID actorId);
    AttendanceResponse update(UUID id, UpdateAttendanceRequest request, UUID actorId);
    List<AttendanceAuditLogResponse> getHistory(UUID id);
}
