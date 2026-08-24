package org.group1.coffeeshopapi.event.dto.request;

import org.group1.coffeeshopapi.common.enums.Status;

import java.time.LocalDateTime;

public record UpdateEventRequest(
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Status status
) {
}