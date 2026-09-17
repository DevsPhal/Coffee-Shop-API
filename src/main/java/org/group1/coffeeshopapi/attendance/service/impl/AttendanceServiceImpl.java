package org.group1.coffeeshopapi.attendance.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.attendance.dto.request.CreateAttendanceRequest;
import org.group1.coffeeshopapi.attendance.dto.request.UpdateAttendanceRequest;
import org.group1.coffeeshopapi.attendance.dto.response.AttendanceAuditLogResponse;
import org.group1.coffeeshopapi.attendance.dto.response.AttendanceResponse;
import org.group1.coffeeshopapi.attendance.entity.Attendance;
import org.group1.coffeeshopapi.attendance.entity.AttendanceAuditLog;
import org.group1.coffeeshopapi.attendance.mapper.AttendanceAuditLogMapper;
import org.group1.coffeeshopapi.attendance.mapper.AttendanceMapper;
import org.group1.coffeeshopapi.attendance.repository.AttendanceAuditLogRepository;
import org.group1.coffeeshopapi.attendance.repository.AttendanceRepository;
import org.group1.coffeeshopapi.attendance.service.AttendanceService;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.common.enums.AttendanceAuditAction;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceAuditLogRepository attendanceAuditLogRepository;
    private final BaristaRepository baristaRepository;
    private final AttendanceMapper attendanceMapper;
    private final AttendanceAuditLogMapper attendanceAuditLogMapper;
    private final ActorLookupService actorLookupService;

    @Override
    @Transactional
    public AttendanceResponse checkIn(UUID baristaId) {
        Barista barista = lockStaff(baristaId);
        if (barista.getStatus() != org.group1.coffeeshopapi.common.enums.UserStatus.ACTIVE) {
            throw new InvalidOperationException("Only active staff can check in");
        }
        if (attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId).isPresent()) {
            throw new InvalidOperationException("You already have an open shift — check out first");
        }

        Attendance attendance = new Attendance();
        attendance.setBarista(barista);
        attendance.setCheckInAt(LocalDateTime.now());
        attendance = attendanceRepository.save(attendance);
        logAudit(attendance, AttendanceAuditAction.CHECK_IN, baristaId, null);

        return attendanceMapper.toResponse(attendance);
    }

    @Override
    @Transactional
    public AttendanceResponse checkOut(UUID baristaId) {
        lockStaff(baristaId);
        Attendance attendance = attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)
                .orElseThrow(() -> new InvalidOperationException("No active shift to check out of"));

        LocalDateTime now = LocalDateTime.now();
        attendance.setCheckOutAt(now);
        attendance.setWorkedMinutes(Duration.between(attendance.getCheckInAt(), now).toMinutes());
        attendance = attendanceRepository.save(attendance);
        logAudit(attendance, AttendanceAuditAction.CHECK_OUT, baristaId, null);

        return attendanceMapper.toResponse(attendance);
    }

    @Override
    public AttendanceResponse getCurrentOpenShift(UUID baristaId) {
        return attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)
                .map(attendanceMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No active shift"));
    }

    @Override
    public Page<AttendanceResponse> listOwn(UUID baristaId, Pageable pageable) {
        return attendanceRepository.findByBaristaId(baristaId, pageable).map(attendanceMapper::toResponse);
    }

    @Override
    public Page<AttendanceResponse> listAll(UUID baristaId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidOperationException("The start date must be before the end date");
        }
        Page<Attendance> records = attendanceRepository.findAll((root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (baristaId != null) predicates.add(builder.equal(root.get("barista").get("id"), baristaId));
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("checkInAt"), from));
            if (to != null) predicates.add(builder.lessThanOrEqualTo(root.get("checkInAt"), to));
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        }, pageable);
        return records.map(attendanceMapper::toResponse);
    }

    @Override
    public AttendanceResponse getById(UUID id) {
        return attendanceMapper.toResponse(findById(id));
    }

    @Override
    @Transactional
    public AttendanceResponse create(CreateAttendanceRequest request, UUID actorId) {
        Barista barista = lockStaff(request.baristaId());
        validateTimes(barista.getId(), null, request.checkInAt(), request.checkOutAt());

        Attendance attendance = new Attendance();
        attendance.setBarista(barista);
        attendance.setCheckInAt(request.checkInAt());
        attendance.setCheckOutAt(request.checkOutAt());
        attendance.setWorkedMinutes(computeWorkedMinutes(request.checkInAt(), request.checkOutAt()));
        attendance.setNote(request.note());
        attendance = attendanceRepository.save(attendance);
        logAudit(attendance, AttendanceAuditAction.ADMIN_CREATED, actorId, request.note());

        return attendanceMapper.toResponse(attendance);
    }

    @Override
    @Transactional
    public AttendanceResponse update(UUID id, UpdateAttendanceRequest request, UUID actorId) {
        Attendance attendance = findById(id);
        lockStaff(attendance.getBarista().getId());

        if (request.checkInAt() != null) {
            attendance.setCheckInAt(request.checkInAt());
        }
        if (request.checkOutAt() != null) {
            attendance.setCheckOutAt(request.checkOutAt());
        }
        if (request.note() != null) {
            attendance.setNote(request.note());
        }
        validateTimes(attendance.getBarista().getId(), attendance.getId(), attendance.getCheckInAt(), attendance.getCheckOutAt());
        attendance.setWorkedMinutes(computeWorkedMinutes(attendance.getCheckInAt(), attendance.getCheckOutAt()));
        attendance = attendanceRepository.save(attendance);
        logAudit(attendance, AttendanceAuditAction.ADMIN_CORRECTED, actorId, request.note());

        return attendanceMapper.toResponse(attendance);
    }

    @Override
    public List<AttendanceAuditLogResponse> getHistory(UUID id) {
        findById(id); // 404s if the record doesn't exist
        List<AttendanceAuditLog> logs = attendanceAuditLogRepository.findByAttendanceIdOrderByCreatedAtAsc(id);

        Set<UUID> actorIds = new HashSet<>();
        for (AttendanceAuditLog log : logs) {
            actorIds.add(log.getActorId());
        }
        Map<UUID, ActorSummary> actors = actorLookupService.resolveAll(actorIds);

        return logs.stream()
                .map(log -> attendanceAuditLogMapper.toResponse(log, actors.get(log.getActorId())))
                .toList();
    }

    private Long computeWorkedMinutes(LocalDateTime checkInAt, LocalDateTime checkOutAt) {
        return checkOutAt != null ? Duration.between(checkInAt, checkOutAt).toMinutes() : null;
    }

    private Barista lockStaff(UUID id) {
        return baristaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
    }

    private void validateTimes(UUID staffId, UUID excludedId, LocalDateTime checkIn, LocalDateTime checkOut) {
        LocalDateTime now = LocalDateTime.now();
        if (checkIn == null) {
            throw new InvalidOperationException("Check-in time is required");
        }
        // Attendance records what already happened, so neither time may be in the future.
        if (checkIn.isAfter(now)) {
            throw new InvalidOperationException(
                    "Check-in time cannot be in the future — the shift has not started yet");
        }
        if (checkOut != null && checkOut.isAfter(now)) {
            throw new InvalidOperationException(
                    "Check-out time cannot be in the future — leave it empty for a shift that is still running");
        }
        if (checkOut != null && !checkOut.isAfter(checkIn)) {
            throw new InvalidOperationException("Check-out time must be after the check-in time");
        }
        if (attendanceRepository.hasOverlap(staffId, excludedId == null ? new UUID(0, 0) : excludedId,
                checkIn, checkOut == null ? LocalDateTime.of(9999, 12, 31, 23, 59) : checkOut)) {
            throw new InvalidOperationException("This shift overlaps another attendance record for this staff member");
        }
    }

    private Attendance findById(UUID id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found"));
    }

    private void logAudit(Attendance attendance, AttendanceAuditAction action, UUID actorId, String note) {
        AttendanceAuditLog log = new AttendanceAuditLog();
        log.setAttendance(attendance);
        log.setAction(action);
        log.setActorId(actorId);
        log.setNote(note);
        attendanceAuditLogRepository.save(log);
    }
}
