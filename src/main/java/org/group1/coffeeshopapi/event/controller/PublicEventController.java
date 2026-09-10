package org.group1.coffeeshopapi.event.controller;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.event.repository.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class PublicEventController {
    private final EventRepository events;

    public record PublicEventResponse(UUID id, String title, String description, String imageUrl,
                                      LocalDateTime startAt, LocalDateTime endAt) {}

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<PublicEventResponse>> list() {
        var entries = events.findByStatusAndEndAtAfterOrderByStartAtAsc(Status.ACTIVE, LocalDateTime.now())
                .stream().map(event -> new PublicEventResponse(event.getId(), event.getTitle(),
                        event.getDescription(), event.getImageUrl(), event.getStartAt(), event.getEndAt())).toList();
        return ApiResponse.of(HttpStatus.OK, "Success", entries);
    }
}
