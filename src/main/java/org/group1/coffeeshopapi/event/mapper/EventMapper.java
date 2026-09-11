package org.group1.coffeeshopapi.event.mapper;

import org.group1.coffeeshopapi.event.dto.response.CustomerEventResponse;
import org.group1.coffeeshopapi.event.dto.response.EventResponse;
import org.group1.coffeeshopapi.event.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    // createdByAdmin is null both for pre-existing rows and for a change made by the Super Admin
    // (see Event's javadoc) — MapStruct null-checks the nested path automatically, so
    // createdByName/createdByRole simply come out null too in that case.
    @Mapping(target = "id", source = "event.id")
    @Mapping(target = "createdBy", source = "event.createdByAdmin.id")
    @Mapping(target = "createdByName", source = "event.createdByAdmin.fullName")
    @Mapping(target = "createdByRole", source = "event.createdByAdmin.role")
    EventResponse toResponse(Event event);

    // Strips staff audit identity before an event reaches the public — see
    // CustomerEventResponse's javadoc.
    CustomerEventResponse toCustomerResponse(Event event);
}