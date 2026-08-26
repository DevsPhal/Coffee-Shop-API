package org.group1.coffeeshopapi.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.OtpPurpose;

/**
 * {@code email} and {@code loginTicket} are conditionally required depending on
 * {@code purpose} (enforced in the service), so neither is marked {@code @NotBlank} here.
 */
public record ResendOtpRequest(
        @NotNull(message = "Purpose is required")
        OtpPurpose purpose,

        @Email(message = "Email must be valid")
        @Schema(example = "user@gmail.com")
        String email,

        String loginTicket
) {
}