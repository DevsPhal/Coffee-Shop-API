package org.group1.coffeeshopapi.event.mapper;

import org.group1.coffeeshopapi.event.dto.response.EventResponse;
import org.group1.coffeeshopapi.event.entity.Event;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EventMapper {
    EventResponse toResponse(Event event);
}