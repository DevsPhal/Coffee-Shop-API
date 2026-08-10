package org.group1.coffeeshopapi.barista.mapper;

import org.group1.coffeeshopapi.admin.entity.User;
import org.group1.coffeeshopapi.barista.dto.request.BaristaRequest;
import org.group1.coffeeshopapi.barista.dto.response.BaristaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BaristaMapper {

    @Mapping(target = "name", source = "fullName")
    @Mapping(target = "phone", source = "phoneNumber")
    @Mapping(target = "status", expression = "java(user.isEnabled() ? \"ACTIVE\" : \"INACTIVE\")")
    @Mapping(target = "joinDate", expression = "java(user.getCreatedAt() == null ? null : user.getCreatedAt().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))")
    BaristaResponse toResponse(User user);

    @Mapping(target = "fullName", source = "name")
    @Mapping(target = "phoneNumber", source = "phone")
    @Mapping(target = "username", expression = "java(request.getUsername() != null && !request.getUsername().isBlank() ? request.getUsername() : request.getEmail())")
    @Mapping(target = "role", constant = "BARISTA")
    @Mapping(target = "enabled", source = "enabled", defaultValue = "true")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "familyName", ignore = true)
    @Mapping(target = "givenName", ignore = true)
    @Mapping(target = "accountNonExpired", ignore = true)
    @Mapping(target = "accountNonLocked", ignore = true)
    @Mapping(target = "credentialsNonExpired", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toEntity(BaristaRequest request);

    @Mapping(target = "fullName", source = "name")
    @Mapping(target = "phoneNumber", source = "phone")
    @Mapping(target = "username", expression = "java(request.getUsername() != null && !request.getUsername().isBlank() ? request.getUsername() : request.getEmail())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uuid", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "familyName", ignore = true)
    @Mapping(target = "givenName", ignore = true)
    @Mapping(target = "accountNonExpired", ignore = true)
    @Mapping(target = "accountNonLocked", ignore = true)
    @Mapping(target = "credentialsNonExpired", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntity(BaristaRequest request, @MappingTarget User user);
}
