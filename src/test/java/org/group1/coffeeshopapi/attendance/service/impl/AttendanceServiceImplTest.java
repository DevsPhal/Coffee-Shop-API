package org.group1.coffeeshopapi.attendance.service.impl;

import org.group1.coffeeshopapi.attendance.dto.request.AdminCheckInRequest;
import org.group1.coffeeshopapi.attendance.dto.request.AdminCheckOutRequest;
import org.group1.coffeeshopapi.attendance.dto.request.CreateAttendanceRequest;
import org.group1.coffeeshopapi.attendance.entity.Attendance;
import org.group1.coffeeshopapi.attendance.entity.AttendanceAuditLog;
import org.group1.coffeeshopapi.attendance.mapper.AttendanceAuditLogMapper;
import org.group1.coffeeshopapi.attendance.mapper.AttendanceMapper;
import org.group1.coffeeshopapi.attendance.repository.AttendanceAuditLogRepository;
import org.group1.coffeeshopapi.attendance.repository.AttendanceRepository;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.barista.repository.BaristaRepository;
import org.group1.coffeeshopapi.common.enums.AttendanceAuditAction;
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

        service.checkIn(baristaId, null);

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

        assertThatThrownBy(() -> service.checkIn(baristaId, null)).isInstanceOf(InvalidOperationException.class);
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void checkInRejectsABaristaWhoAlreadyHasAnOpenShift() {
        UUID baristaId = UUID.randomUUID();
        Barista barista = activeBarista(baristaId);
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(barista));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId))
                .thenReturn(Optional.of(new Attendance()));

        assertThatThrownBy(() -> service.checkIn(baristaId, null))
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

        service.checkOut(baristaId, null);

        assertThat(openShift.getCheckOutAt()).isNotNull();
        assertThat(openShift.getWorkedMinutes()).isBetween(89L, 91L);
    }

    @Test
    void checkOutWithNoOpenShiftIsRejected() {
        UUID baristaId = UUID.randomUUID();
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(activeBarista(baristaId)));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkOut(baristaId, null)).isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void baristaCheckInNoteIsSavedOnTheNewShiftAndInItsAuditEntry() {
        UUID baristaId = UUID.randomUUID();
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(activeBarista(baristaId)));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.checkIn(baristaId, "  Arrived late — bus was delayed  ");

        ArgumentCaptor<Attendance> captor = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceRepository).save(captor.capture());
        assertThat(captor.getValue().getNote()).isEqualTo("Arrived late — bus was delayed");
        AttendanceAuditLog log = capturedAuditLog();
        assertThat(log.getAction()).isEqualTo(AttendanceAuditAction.CHECK_IN);
        assertThat(log.getActorId()).isEqualTo(baristaId);
        assertThat(log.getNote()).isEqualTo("Arrived late — bus was delayed");
    }

    @Test
    void baristaCheckInWithABlankNoteStoresNoNote() {
        UUID baristaId = UUID.randomUUID();
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(activeBarista(baristaId)));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.checkIn(baristaId, "   ");

        ArgumentCaptor<Attendance> captor = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceRepository).save(captor.capture());
        assertThat(captor.getValue().getNote()).isNull();
    }

    @Test
    void baristaCheckOutNoteReplacesTheShiftNote() {
        UUID baristaId = UUID.randomUUID();
        Attendance openShift = openShift(baristaId, LocalDateTime.now().minusHours(4));
        openShift.setNote("Arrived late");
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(openShift.getBarista()));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.of(openShift));
        when(attendanceRepository.save(openShift)).thenReturn(openShift);

        service.checkOut(baristaId, "Arrived late. Leaving early — feeling unwell");

        assertThat(openShift.getCheckOutAt()).isNotNull();
        assertThat(openShift.getNote()).isEqualTo("Arrived late. Leaving early — feeling unwell");
        AttendanceAuditLog log = capturedAuditLog();
        assertThat(log.getAction()).isEqualTo(AttendanceAuditAction.CHECK_OUT);
        assertThat(log.getNote()).isEqualTo("Arrived late. Leaving early — feeling unwell");
    }

    @Test
    void baristaCheckOutWithoutANoteKeepsTheNoteWrittenAtCheckIn() {
        for (String noNote : new String[]{null, "", "   "}) {
            UUID baristaId = UUID.randomUUID();
            Attendance openShift = openShift(baristaId, LocalDateTime.now().minusHours(8));
            openShift.setNote("Arrived late");
            when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(openShift.getBarista()));
            when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.of(openShift));
            when(attendanceRepository.save(openShift)).thenReturn(openShift);

            service.checkOut(baristaId, noNote);

            assertThat(openShift.getCheckOutAt()).isNotNull();
            assertThat(openShift.getNote()).as("check-out note %s", noNote).isEqualTo("Arrived late");
        }
    }

    @Test
    void adminCheckInStartsAnOpenShiftAtTheChosenTimeWithTheAdminAsActor() {
        UUID baristaId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        LocalDateTime checkInAt = LocalDateTime.now().minusMinutes(20).withSecond(0).withNano(0);
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(activeBarista(baristaId)));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.adminCheckIn(new AdminCheckInRequest(baristaId, checkInAt, "Forgot to punch in"), adminId);

        ArgumentCaptor<Attendance> captor = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceRepository).save(captor.capture());
        assertThat(captor.getValue().getCheckInAt()).isEqualTo(checkInAt);
        assertThat(captor.getValue().getCheckOutAt()).isNull();
        assertThat(captor.getValue().getWorkedMinutes()).isNull();
        AttendanceAuditLog log = capturedAuditLog();
        assertThat(log.getAction()).isEqualTo(AttendanceAuditAction.CHECK_IN);
        assertThat(log.getActorId()).isEqualTo(adminId);
        assertThat(log.getNote()).isEqualTo("Forgot to punch in");
    }

    @Test
    void adminCheckInRejectsAStaffMemberWhoIsAlreadyOnShift() {
        UUID baristaId = UUID.randomUUID();
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(activeBarista(baristaId)));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId))
                .thenReturn(Optional.of(new Attendance()));

        assertThatThrownBy(() -> service.adminCheckIn(
                new AdminCheckInRequest(baristaId, LocalDateTime.now().minusMinutes(5), null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already on shift");
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void adminCheckInRejectsAnInactiveStaffMember() {
        UUID baristaId = UUID.randomUUID();
        Barista barista = activeBarista(baristaId);
        barista.setStatus(UserStatus.SUSPENDED);
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(barista));

        assertThatThrownBy(() -> service.adminCheckIn(
                new AdminCheckInRequest(baristaId, LocalDateTime.now().minusMinutes(5), null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("active staff");
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void adminCheckInRejectsATimeInTheFuture() {
        UUID baristaId = UUID.randomUUID();
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(activeBarista(baristaId)));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adminCheckIn(
                new AdminCheckInRequest(baristaId, LocalDateTime.now().plusHours(1), null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("future");
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void adminCheckInRejectsATimeInsideAnEarlierCompletedShift() {
        UUID baristaId = UUID.randomUUID();
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(activeBarista(baristaId)));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.empty());
        when(attendanceRepository.hasOverlap(eq(baristaId), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.adminCheckIn(
                new AdminCheckInRequest(baristaId, LocalDateTime.now().minusHours(2), null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("overlaps");
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void adminCheckOutClosesTheExistingOpenShiftWithoutTouchingItsCheckIn() {
        UUID baristaId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        Barista barista = activeBarista(baristaId);
        LocalDateTime checkInAt = LocalDateTime.now().minusHours(9).withSecond(0).withNano(0);
        LocalDateTime checkOutAt = checkInAt.plusHours(8).plusMinutes(30);
        Attendance openShift = new Attendance();
        openShift.setId(UUID.randomUUID());
        openShift.setBarista(barista);
        openShift.setCheckInAt(checkInAt);
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(barista));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.of(openShift));
        when(attendanceRepository.save(openShift)).thenReturn(openShift);

        service.adminCheckOut(new AdminCheckOutRequest(baristaId, checkOutAt, "Left without punching out"), adminId);

        assertThat(openShift.getCheckInAt()).isEqualTo(checkInAt);
        assertThat(openShift.getCheckOutAt()).isEqualTo(checkOutAt);
        assertThat(openShift.getWorkedMinutes()).isEqualTo(510L);
        verify(attendanceRepository).hasOverlap(baristaId, openShift.getId(), checkInAt, checkOutAt);
        AttendanceAuditLog log = capturedAuditLog();
        assertThat(log.getAction()).isEqualTo(AttendanceAuditAction.CHECK_OUT);
        assertThat(log.getActorId()).isEqualTo(adminId);
    }

    @Test
    void adminCheckOutWithNoActiveShiftIsRejected() {
        UUID baristaId = UUID.randomUUID();
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(activeBarista(baristaId)));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adminCheckOut(
                new AdminCheckOutRequest(baristaId, LocalDateTime.now(), null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("no active shift");
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void adminCheckOutRejectsATimeBeforeTheShiftsCheckIn() {
        UUID baristaId = UUID.randomUUID();
        Attendance openShift = openShift(baristaId, LocalDateTime.now().minusHours(2));
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(openShift.getBarista()));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.of(openShift));

        assertThatThrownBy(() -> service.adminCheckOut(
                new AdminCheckOutRequest(baristaId, openShift.getCheckInAt().minusMinutes(1), null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("after the check-in");
        assertThat(openShift.getCheckOutAt()).isNull();
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void adminCheckOutRejectsATimeInTheFuture() {
        UUID baristaId = UUID.randomUUID();
        Attendance openShift = openShift(baristaId, LocalDateTime.now().minusHours(2));
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(openShift.getBarista()));
        when(attendanceRepository.findByBaristaIdAndCheckOutAtIsNull(baristaId)).thenReturn(Optional.of(openShift));

        assertThatThrownBy(() -> service.adminCheckOut(
                new AdminCheckOutRequest(baristaId, LocalDateTime.now().plusHours(1), null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("future");
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void adminBackfillRequiresACheckOutTimeSoItCannotStartAnOpenShift() {
        UUID baristaId = UUID.randomUUID();
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(activeBarista(baristaId)));

        CreateAttendanceRequest request = new CreateAttendanceRequest(
                baristaId, LocalDateTime.now().minusHours(3), null, null);

        assertThatThrownBy(() -> service.create(request, UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("check-out time");
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    void adminBackfillRejectsACheckInTimeInTheFuture() {
        UUID baristaId = UUID.randomUUID();
        when(baristaRepository.findByIdForUpdate(baristaId)).thenReturn(Optional.of(activeBarista(baristaId)));

        LocalDateTime checkIn = LocalDateTime.now().plusHours(1);
        CreateAttendanceRequest request = new CreateAttendanceRequest(
                baristaId, checkIn, checkIn.plusHours(1), null);

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

    private Attendance openShift(UUID baristaId, LocalDateTime checkInAt) {
        Attendance attendance = new Attendance();
        attendance.setId(UUID.randomUUID());
        attendance.setBarista(activeBarista(baristaId));
        attendance.setCheckInAt(checkInAt);
        return attendance;
    }

    private AttendanceAuditLog capturedAuditLog() {
        ArgumentCaptor<AttendanceAuditLog> captor = ArgumentCaptor.forClass(AttendanceAuditLog.class);
        verify(attendanceAuditLogRepository).save(captor.capture());
        return captor.getValue();
    }
}
