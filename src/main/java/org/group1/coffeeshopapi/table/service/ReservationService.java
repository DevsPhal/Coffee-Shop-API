package org.group1.coffeeshopapi.table.service;

import org.group1.coffeeshopapi.common.enums.ReservationStatus;
import org.group1.coffeeshopapi.table.dto.request.CheckInReservationRequest;
import org.group1.coffeeshopapi.table.dto.request.CloseReservationRequest;
import org.group1.coffeeshopapi.table.dto.request.ReservationRequest;
import org.group1.coffeeshopapi.table.dto.response.ReservationResponse;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ReservationService {

    // Bookings starting on the given day (today when empty), earliest first.
    List<ReservationResponse> list(LocalDate date, ReservationStatus status, UUID tableId);

    ReservationResponse getById(UUID id);

    // Each write locks the table row first, so two staff booking the same slot can't both succeed.
    ReservationResponse create(ReservationRequest request, UUID actorId);

    ReservationResponse update(UUID id, ReservationRequest request, UUID actorId);

    // The booked party arrived: the table becomes occupied and the booking seated, ready for their order.
    ReservationResponse checkIn(UUID id, CheckInReservationRequest request, UUID actorId, boolean mayOverride);

    ReservationResponse cancel(UUID id, CloseReservationRequest request, UUID actorId);

    ReservationResponse markNoShow(UUID id, CloseReservationRequest request, UUID actorId);

    // For the tables screen: per table, the seated booking and bookings starting in the next 24 hours.
    Map<UUID, List<ReservationResponse>> currentByTable(Collection<UUID> tableIds);

    // Scheduler: bookings whose hold window has started, and booked parties late enough to be no-shows.
    List<UUID> listDueForHold();

    List<UUID> listLateForNoShow();

    void holdTableFor(UUID reservationId);

    void expireIfLate(UUID reservationId);
}
