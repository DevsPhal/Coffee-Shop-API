package org.group1.coffeeshopapi.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;

// Staff invited over Telegram have no email or password — they sign in with the phone number
// the invite was created for, and the code goes to the Telegram chat that accepted it.
public record PhoneLoginRequest(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = ValidationPatterns.CAMBODIA_PHONE_REGEX, message = ValidationPatterns.CAMBODIA_PHONE_MESSAGE)
        @Schema(example = "072 345 5674")
        String phoneNumber
) {
}
