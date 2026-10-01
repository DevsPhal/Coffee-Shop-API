package org.group1.coffeeshopapi.event.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerEventResponse(
        UUID id,
        String title,
        String description,
        String imageUrl,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
