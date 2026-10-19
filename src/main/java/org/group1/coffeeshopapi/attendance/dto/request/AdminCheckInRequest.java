package org.group1.coffeeshopapi.attendance.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminCheckInRequest(
        @NotNull(message = "Staff member is required")
        UUID baristaId,

        @NotNull(message = "Check-in time is required")
        LocalDateTime checkInAt,

        @Size(max = 255) String note
) {
}
