package org.group1.coffeeshopapi.event.mapper;

import org.group1.coffeeshopapi.event.dto.response.EventResponse;
import org.group1.coffeeshopapi.event.entity.Event;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "createdByName", source = "createdByActor.name")
    @Mapping(target = "createdByRole", source = "createdByActor.role")
    EventResponse toResponse(Event event, ActorSummary createdByActor);
}