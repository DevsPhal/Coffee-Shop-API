package org.group1.coffeeshopapi.admin.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.enums.Gender;

public record CreateStaffRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @Pattern(regexp = "\\d{9,10}", message = "Phone number must be 9 or 10 digits")
        String phoneNumber,

        Gender gender
) {
}