package org.group1.coffeeshopapi.attendance.dto.request;

import jakarta.validation.constraints.Size;

/** Optional body for a barista's own check-in or check-out: a short note about the shift. */
public record AttendanceNoteRequest(
        @Size(max = 255, message = "Note must be 255 characters or fewer")
        String note
) {
}
