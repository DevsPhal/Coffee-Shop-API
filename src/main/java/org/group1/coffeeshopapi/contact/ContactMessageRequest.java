package org.group1.coffeeshopapi.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactMessageRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 30) String phone,
        @NotBlank @Size(max = 100) String topic,
        @NotBlank @Size(min = 5, max = 5000) String message
) {}
