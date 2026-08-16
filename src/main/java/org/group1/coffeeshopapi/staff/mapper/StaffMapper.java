package org.group1.coffeeshopapi.staff.mapper;

import org.group1.coffeeshopapi.staff.dto.request.StaffRequest;
import org.group1.coffeeshopapi.staff.dto.response.StaffResponse;
import org.group1.coffeeshopapi.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StaffMapper {

    @Mapping(target = "name", source = "fullName")
    @Mapping(target = "phone", source = "phoneNumber")
    @Mapping(target = "status", expression = "java(user.isEnabled() ? \"ACTIVE\" : \"INACTIVE\")")
    @Mapping(target = "joinDate", expression = "java(user.getCreatedAt() == null ? null : user.getCreatedAt().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))")
    StaffResponse toResponse(User user);

    @Mapping(target = "givenName", source = "fullName")
    @Mapping(target = "familyName", constant = "")
    @Mapping(target = "username", expression = "java(request.getUsername() != null && !request.getUsername().isBlank() ? request.getUsername() : request.getEmail())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "dob", ignore = true)
    @Mapping(target = "profileImage", ignore = true)
    @Mapping(target = "coverImage", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "loyaltyPoints", ignore = true)
    @Mapping(target = "notificationPreference", ignore = true)
    @Mapping(target = "telegramChatId", ignore = true)
    @Mapping(target = "accountNonExpired", ignore = true)
    @Mapping(target = "accountNonLocked", ignore = true)
    @Mapping(target = "credentialsNonExpired", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(StaffRequest request);

    @Mapping(target = "givenName", source = "fullName")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "familyName", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "dob", ignore = true)
    @Mapping(target = "profileImage", ignore = true)
    @Mapping(target = "coverImage", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "loyaltyPoints", ignore = true)
    @Mapping(target = "notificationPreference", ignore = true)
    @Mapping(target = "telegramChatId", ignore = true)
    @Mapping(target = "accountNonExpired", ignore = true)
    @Mapping(target = "accountNonLocked", ignore = true)
    @Mapping(target = "credentialsNonExpired", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(StaffRequest request, @MappingTarget User user);
}
