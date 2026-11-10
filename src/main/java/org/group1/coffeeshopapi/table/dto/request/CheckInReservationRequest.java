package org.group1.coffeeshopapi.table.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

// The booked party has arrived. guestCount defaults to the booked party size.
public record CheckInReservationRequest(
        @Min(value = 1, message = "At least 1 guest must be seated")
        Integer guestCount,
        // Admin only: seat the party even though another booking is holding the table.
        Boolean override,
        @Size(max = 300, message = "The override reason must not exceed 300 characters")
        String overrideReason
) {
    public static CheckInReservationRequest empty() {
        return new CheckInReservationRequest(null, null, null);
    }

    public boolean overrideRequested() {
        return Boolean.TRUE.equals(override);
    }
}
