package org.group1.coffeeshopapi.table.dto.request;

import jakarta.validation.constraints.Size;

// Cancelling a booking or marking it as a no-show.
public record CloseReservationRequest(
        @Size(max = 300, message = "Reason must not exceed 300 characters")
        String reason
) {
    public static CloseReservationRequest empty() {
        return new CloseReservationRequest(null);
    }
}
