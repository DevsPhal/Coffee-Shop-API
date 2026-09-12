package org.group1.coffeeshopapi.event.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public projection of {@link EventResponse} — deliberately excludes staff audit identity, same
 * as {@code CustomerProductResponse} does for products. Public rather than customer-only: the
 * Telegram bot's {@code /events} command already serves this same content to anyone, linked
 * account or not (see {@code EventsCommand}).
 */
public record CustomerEventResponse(
        UUID id,
        String title,
        String description,
        String imageUrl,
        LocalDateTime startAt,
        LocalDateTime endAt
) {
}
