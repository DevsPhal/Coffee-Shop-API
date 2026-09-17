package org.group1.coffeeshopapi.event.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// Public view of an event — excludes staff identity. Anyone can see it, linked account or not.
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
