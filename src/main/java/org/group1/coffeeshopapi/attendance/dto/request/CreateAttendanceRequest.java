package org.group1.coffeeshopapi.attendance.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

// Admin-only: backfills a shift a barista forgot to punch (or punched outside the app entirely).
// checkOutAt may be left null to record an open (still ongoing) shift.
public record CreateAttendanceRequest(
        @NotNull(message = "Barista is required")
        UUID baristaId,

        @NotNull(message = "Check-in time is required")
        LocalDateTime checkInAt,

        LocalDateTime checkOutAt,
        String note
) {
}
