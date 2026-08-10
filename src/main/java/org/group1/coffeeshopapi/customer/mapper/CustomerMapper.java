package org.group1.coffeeshopapi.customer.mapper;

import org.group1.coffeeshopapi.admin.entity.User;
import org.group1.coffeeshopapi.customer.dto.request.CustomerRequest;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public User toEntity(CustomerRequest request) {
        User user = new User();
        user.setGivenName(request.getFirstName());
        user.setFamilyName(request.getFamilyName());
        user.setFullName(((request.getFirstName() == null ? "" : request.getFirstName()) + " " + (request.getFamilyName() == null ? "" : request.getFamilyName())).trim());
        user.setUsername(request.getUsername() != null && !request.getUsername().isBlank() ? request.getUsername() : request.getEmail());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhone());
        user.setAddress(request.getAddress());
        return user;
    }

    public void updateEntity(CustomerRequest request, User user) {
        if (request.getFirstName() != null) {
            user.setGivenName(request.getFirstName());
        }
        if (request.getFamilyName() != null) {
            user.setFamilyName(request.getFamilyName());
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhoneNumber(request.getPhone());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        String fullName = ((user.getGivenName() == null ? "" : user.getGivenName()) + " " + (user.getFamilyName() == null ? "" : user.getFamilyName())).trim();
        user.setFullName(fullName);
    }
}
