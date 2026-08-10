package org.group1.coffeeshopapi.admin.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCreateUserRequest(
        @NotBlank @Size(
   min = 3,
   max = 64
) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(
   min = 6
) String password,
        @NotBlank String givenName,
        @NotBlank String familyName,
        String phoneNumber, String role) {
}
