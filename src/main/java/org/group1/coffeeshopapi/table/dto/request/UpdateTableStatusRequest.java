package org.group1.coffeeshopapi.table.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.TableStatus;

public record UpdateTableStatusRequest(
        @NotNull(message = "Table status is required")
        TableStatus status,
        @Min(value = 0, message = "Guest count must not be negative")
        Integer guestCount,
        // Admin only: make a change the table rules would block. The reason is required and kept in the history.
        Boolean override,
        @Size(max = 300, message = "The override reason must not exceed 300 characters")
        String overrideReason
) {
    public UpdateTableStatusRequest(TableStatus status, Integer guestCount) {
        this(status, guestCount, null, null);
    }

    public boolean overrideRequested() {
        return Boolean.TRUE.equals(override);
    }
}
