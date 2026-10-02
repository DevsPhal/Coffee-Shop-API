package org.group1.coffeeshopapi.table.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.TableStatus;

public record UpdateTableStatusRequest(
        @NotNull(message = "Table status is required")
        TableStatus status,
        @Min(value = 0, message = "Guest count must not be negative")
        Integer guestCount
) {
}
