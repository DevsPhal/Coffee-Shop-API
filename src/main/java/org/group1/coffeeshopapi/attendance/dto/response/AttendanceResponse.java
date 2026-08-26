package org.group1.coffeeshopapi.attendance.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UUID baristaId,
        String baristaName,
        LocalDateTime checkInAt,
        LocalDateTime checkOutAt,
        Long workedMinutes,
        boolean open,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
