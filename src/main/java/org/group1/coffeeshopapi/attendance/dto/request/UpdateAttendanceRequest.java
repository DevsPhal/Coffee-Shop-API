package org.group1.coffeeshopapi.attendance.dto.request;

import java.time.LocalDateTime;
import jakarta.validation.constraints.Size;

public record UpdateAttendanceRequest(
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        @Size(max = 255, message = "Note must not exceed 255 characters") String note
) {
}
