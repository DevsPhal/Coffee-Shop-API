package org.group1.coffeeshopapi.events.dto.request;

import java.time.LocalDate;

import org.group1.coffeeshopapi.common.enums.EventStatus;
import org.group1.coffeeshopapi.common.enums.EventType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Event type is required")
    private EventType type;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private String description;

    private EventStatus status;
}
