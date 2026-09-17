package org.group1.coffeeshopapi.attendance.service.impl;

import org.group1.coffeeshopapi.attendance.dto.request.CreateAttendanceRequest;
import org.group1.coffeeshopapi.attendance.entity.Attendance;
import org.group1.coffeeshopapi.attendance.mapper.AttendanceAuditLogMapper;
import org.group1.coffeeshopapi.attendance.mapper.AttendanceMapper;
import org.group1.coffeeshopapi.attendance.repository.AttendanceAuditLogRepository;
import org.group1.coffeeshopapi.attendance.repository.AttendanceRepository;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.common.enums.UserStatus;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Covers the shift check-in/check-out state machine and the future-time/overlap validation on
// admin-entered records.
@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private AttendanceAuditLogRepository attendanceAuditLogRepository;
    @Mock private BaristaRepository baristaRepository;
    @Mock private AttendanceMapper attendanceMapper;
    @Mock private AttendanceAuditLogMapper attendanceAuditLogMapper;
    @Mock private ActorLookupService actorLookupService;
    @InjectMocks private AttendanceServiceImpl service;

    @Test
    void checkInStartsAnOpenShiftForAnActiveBaristaWithNoneAlreadyOpen() {
        UUID baristaId = UUID.randomUUID();
        Barista barista = activeBarista(baristaId);
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(barista));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.checkIn(baristaId);

        ArgumentCaptor<Attendance> captor = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceRepository).save(captor.capture());
        assertThat(captor.getValue().getCheckInAt()).isNotNull();
        assertThat(captor.getValue().getCheckOutAt()).isNull();
    }

    @Test
    void checkInRejectsAnInactiveBarista() {
        UUID baristaId = UUID.randomUUID();
        Barista barista = new Barista();
        barista.setId(baristaId);
        barista.setStatus(UserStatus.SUSPENDED);
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(barista));

        assertThatThrownBy(() -> service.checkIn(baristaId)).isInstanceOf(InvalidOperationException.class);
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void checkInRejectsABaristaWhoAlreadyHasAnOpenShift() {
        UUID baristaId = UUID.randomUUID();
        Barista barista = activeBarista(baristaId);
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(barista));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId))
                .thenReturn(Optional.of(new Attendance()));

        assertThatThrownBy(() -> service.checkIn(baristaId))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already have an open shift");
    }

    @Test
    void checkOutComputesWorkedMinutesFromTheOpenShiftsCheckInTime() {
        UUID baristaId = UUID.randomUUID();
        Barista barista = activeBarista(baristaId);
        Attendance openShift = new Attendance();
        openShift.setBarista(barista);
        openShift.setCheckInAt(LocalDateTime.now().minusMinutes(90));

        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(barista));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.of(openShift));
        when(attendanceRepository.save(openShift)).thenReturn(openShift);

        service.checkOut(baristaId);

        assertThat(openShift.getCheckOutAt()).isNotNull();
        assertThat(openShift.getWorkedMinutes()).isBetween(89L, 91L);
    }

    @Test
    void checkOutWithNoOpenShiftIsRejected() {
        UUID baristaId = UUID.randomUUID();
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(activeBarista(baristaId)));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkOut(baristaId)).isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void adminBackfillRejectsACheckInTimeInTheFuture() {
        UUID baristaId = UUID.randomUUID();
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(activeBarista(baristaId)));

        CreateAttendanceRequest request = new CreateAttendanceRequest(
                baristaId, LocalDateTime.now().plusHours(1), null, null);

        assertThatThrownBy(() -> service.create(request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("future");
    }

    @Test
    void adminBackfillRejectsACheckOutTimeBeforeTheCheckInTime() {
        UUID baristaId = UUID.randomUUID();
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(activeBarista(baristaId)));

        LocalDateTime checkIn = LocalDateTime.now().minusHours(2);
        CreateAttendanceRequest request = new CreateAttendanceRequest(
                baristaId, checkIn, checkIn.minusMinutes(30), null);

        assertThatThrownBy(() -> service.create(request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("after the check-in");
    }

    @Test
    void adminBackfillRejectsAShiftThatOverlapsAnExistingRecordForTheSameBarista() {
        UUID baristaId = UUID.randomUUID();
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(activeBarista(baristaId)));
        when(attendanceRepository.hasOverlap(eq(baristaId), any(), any(), any())).thenReturn(true);

        LocalDateTime checkIn = LocalDateTime.now().minusHours(3);
        CreateAttendanceRequest request = new CreateAttendanceRequest(
                baristaId, checkIn, checkIn.plusHours(1), null);

        assertThatThrownBy(() -> service.create(request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("overlaps");
    }

    private Barista activeBarista(UUID id) {
        Barista barista = new Barista();
        barista.setId(id);
        barista.setStatus(UserStatus.ACTIVE);
        return barista;
    }
}
