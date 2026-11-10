package org.group1.coffeeshopapi.table.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

// Book a table, or change a booking that has not been seated yet.
public record ReservationRequest(
        @NotNull(message = "Choose a table")
        UUID tableId,

        @NotBlank(message = "Enter the guest name")
        @Size(max = 100, message = "Guest name must not exceed 100 characters")
        String customerName,

        @Size(max = 30, message = "Phone number must not exceed 30 characters")
        @Pattern(regexp = "^$|^\\+?[0-9][0-9 ()-]{5,28}$", message = "Enter a valid phone number")
        String customerPhone,

        @NotNull(message = "Enter the number of guests")
        @Min(value = 1, message = "A booking needs at least 1 guest")
        @Max(value = 100, message = "A booking can have at most 100 guests")
        Integer partySize,

        // Shop time (Asia/Phnom_Penh).
        @NotNull(message = "Choose the date and time")
        LocalDateTime reservedFor,

        // How long the party keeps the table; the shop default when empty.
        @Min(value = 15, message = "A booking lasts at least 15 minutes")
        @Max(value = 480, message = "A booking lasts at most 8 hours")
        Integer durationMinutes,

        @Size(max = 500, message = "Note must not exceed 500 characters")
        String note
) {
}
