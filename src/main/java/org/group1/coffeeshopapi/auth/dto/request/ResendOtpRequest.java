package org.group1.coffeeshopapi.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import org.group1.coffeeshopapi.common.enums.OtpPurpose;

public record ResendOtpRequest(
        @NotNull(message = "Purpose is required")
        OtpPurpose purpose,

        String email,

        String loginTicket
) {
}