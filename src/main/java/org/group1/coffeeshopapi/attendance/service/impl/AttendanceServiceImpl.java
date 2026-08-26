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
        if (attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId).isPresent()) {
            throw new InvalidOperationException("You already have an open shift — check out first");
        }

        Attendance attendance = new Attendance();
        attendance.setBarista(baristaRepository.getReferenceById(baristaId));
        attendance.setCheckInAt(LocalDateTime.now());
        attendance = attendanceRepository.save(attendance);
        logAudit(attendance, AttendanceAuditAction.CHECK_IN, baristaId, null);

        return attendanceMapper.toResponse(attendance);
    }

    @Override
    @Transactional
    public AttendanceResponse checkOut(UUID baristaId) {
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
        Page<Attendance> records;
        if (baristaId != null && from != null && to != null) {
            records = attendanceRepository.findByBaristaIdAndCheckInAtBetween(baristaId, from, to, pageable);
        } else if (baristaId != null) {
            records = attendanceRepository.findByBaristaId(baristaId, pageable);
        } else if (from != null && to != null) {
            records = attendanceRepository.findByCheckInAtBetween(from, to, pageable);
        } else {
            records = attendanceRepository.findAll(pageable);
        }
        return records.map(attendanceMapper::toResponse);
    }

    @Override
    public AttendanceResponse getById(UUID id) {
        return attendanceMapper.toResponse(findById(id));
    }

    @Override
    @Transactional
    public AttendanceResponse create(CreateAttendanceRequest request, UUID actorId) {
        Barista barista = baristaRepository.findById(request.baristaId())
                .orElseThrow(() -> new ResourceNotFoundException("Barista not found"));
        if (request.checkOutAt() != null && !request.checkOutAt().isAfter(request.checkInAt())) {
            throw new InvalidOperationException("Check-out time must be after the check-in time");
        }

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

        if (request.checkInAt() != null) {
            attendance.setCheckInAt(request.checkInAt());
        }
        if (request.checkOutAt() != null) {
            attendance.setCheckOutAt(request.checkOutAt());
        }
        if (request.note() != null) {
            attendance.setNote(request.note());
        }
        if (attendance.getCheckOutAt() != null && !attendance.getCheckOutAt().isAfter(attendance.getCheckInAt())) {
            throw new InvalidOperationException("Check-out time must be after the check-in time");
        }
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
