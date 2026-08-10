package org.group1.coffeeshopapi.events.mapper;

import java.util.List;

import org.group1.coffeeshopapi.events.dto.request.EventRequest;
import org.group1.coffeeshopapi.events.dto.response.EventResponse;
import org.group1.coffeeshopapi.events.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventMapper {

    EventResponse toResponse(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", source = "status", defaultValue = "UPCOMING")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Event toEntity(EventRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(EventRequest request, @MappingTarget Event event);

    List<EventResponse> toResponses(List<Event> events);
}
