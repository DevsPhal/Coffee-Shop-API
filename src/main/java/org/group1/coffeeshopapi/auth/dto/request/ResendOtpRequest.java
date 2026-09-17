package org.group1.coffeeshopapi.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.OtpPurpose;

// email and loginTicket are each required depending on purpose — checked in the service instead.
public record ResendOtpRequest(
        @NotNull(message = "Purpose is required")
        OtpPurpose purpose,

        @Email(message = "Email must be valid")
        @Schema(example = "user@gmail.com")
        String email,

        String loginTicket
) {
}