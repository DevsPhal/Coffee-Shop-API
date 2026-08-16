package org.group1.coffeeshopapi.customer.mapper;

import org.group1.coffeeshopapi.customer.dto.request.CustomerRequest;
import org.group1.coffeeshopapi.customer.dto.response.CustomerResponse;
import org.group1.coffeeshopapi.user.entity.User;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CustomerMapper {

    @Mapping(target = "name", source = "fullName")
    @Mapping(target = "phone", source = "phoneNumber")
    @Mapping(target = "totalOrders", constant = "0L")
    @Mapping(target = "totalSpent", constant = "0.0D")
    @Mapping(target = "createdAt", expression = "java(user.getCreatedAt() == null ? null : user.getCreatedAt().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))")
    @Mapping(target = "status", expression = "java(user.isEnabled() ? \"ACTIVE\" : \"INACTIVE\")")
    @Mapping(target = "locked", expression = "java(!user.isEnabled())")
    CustomerResponse toResponse(User user);

    @Mapping(target = "givenName", source = "firstName")
    @Mapping(target = "phoneNumber", source = "phone")
    @Mapping(target = "fullName", expression = "java(fullName(request.getFirstName(), request.getFamilyName()))")
    @Mapping(target = "username", expression = "java(request.getUsername() != null && !request.getUsername().isBlank() ? request.getUsername() : request.getEmail())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "dob", ignore = true)
    @Mapping(target = "profileImage", ignore = true)
    @Mapping(target = "coverImage", ignore = true)
    @Mapping(target = "loyaltyPoints", ignore = true)
    @Mapping(target = "notificationPreference", ignore = true)
    @Mapping(target = "telegramChatId", ignore = true)
    @Mapping(target = "accountNonExpired", ignore = true)
    @Mapping(target = "accountNonLocked", ignore = true)
    @Mapping(target = "credentialsNonExpired", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(CustomerRequest request);

    @Mapping(target = "givenName", source = "firstName")
    @Mapping(target = "phoneNumber", source = "phone")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fullName", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "gender", ignore = true)
    @Mapping(target = "dob", ignore = true)
    @Mapping(target = "profileImage", ignore = true)
    @Mapping(target = "coverImage", ignore = true)
    @Mapping(target = "loyaltyPoints", ignore = true)
    @Mapping(target = "notificationPreference", ignore = true)
    @Mapping(target = "telegramChatId", ignore = true)
    @Mapping(target = "accountNonExpired", ignore = true)
    @Mapping(target = "accountNonLocked", ignore = true)
    @Mapping(target = "credentialsNonExpired", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CustomerRequest request, @MappingTarget User user);

    @AfterMapping
    default void applyCustomerDerivedFields(CustomerRequest request, @MappingTarget User user) {
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
        }
        user.setFullName(fullName(user.getGivenName(), user.getFamilyName()));
    }

    default String fullName(String firstName, String familyName) {
        return ((firstName == null ? "" : firstName) + " " + (familyName == null ? "" : familyName)).trim();
    }
}
