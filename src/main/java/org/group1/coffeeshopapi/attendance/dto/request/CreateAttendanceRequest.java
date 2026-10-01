package org.group1.coffeeshopapi.attendance.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateAttendanceRequest(
        @NotNull(message = "Barista is required")
        UUID baristaId,

        @NotNull(message = "Check-in time is required")
        LocalDateTime checkInAt,

        LocalDateTime checkOutAt,
        @Size(max = 255) String note
) {
}
