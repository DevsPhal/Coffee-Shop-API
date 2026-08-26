package org.group1.coffeeshopapi.attendance.dto.request;

import java.time.LocalDateTime;

// Admin-only: corrects an existing attendance record (e.g. a barista forgot to check out, or
// mistyped nothing but the times still need adjusting). Null fields are left unchanged.
public record UpdateAttendanceRequest(
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        String note
) {
}
