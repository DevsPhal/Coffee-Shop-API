package org.group1.coffeeshopapi.attendance.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminCheckOutRequest(
        @NotNull(message = "Staff member is required")
        UUID baristaId,

        @NotNull(message = "Check-out time is required")
        LocalDateTime checkOutAt,

        @Size(max = 255) String note
) {
}
