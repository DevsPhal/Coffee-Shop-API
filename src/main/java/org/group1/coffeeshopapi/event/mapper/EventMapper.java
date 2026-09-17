package org.group1.coffeeshopapi.event.mapper;

import org.group1.coffeeshopapi.event.dto.response.CustomerEventResponse;
import org.group1.coffeeshopapi.event.dto.response.EventResponse;
import org.group1.coffeeshopapi.event.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "createdBy", source = "event.createdByAdmin.id")
    @Mapping(target = "createdByName", source = "event.createdByAdmin.fullName")
    @Mapping(target = "createdByRole", source = "event.createdByAdmin.role")
    EventResponse toResponse(Event event);

    // Strips staff identity before an event reaches the public.
    CustomerEventResponse toCustomerResponse(Event event);
}