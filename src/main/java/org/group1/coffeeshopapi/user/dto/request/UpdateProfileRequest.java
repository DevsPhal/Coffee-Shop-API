package org.group1.coffeeshopapi.user.dto.request;

import jakarta.validation.constraints.Pattern;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;
import org.group1.coffeeshopapi.common.enums.Gender;

// Self-service profile edit — email and password change through their own dedicated flows
// (verification/OTP), so they're deliberately not here.
public record UpdateProfileRequest(
        String fullName,

        @Pattern(regexp = ValidationPatterns.CAMBODIA_PHONE_REGEX, message = ValidationPatterns.CAMBODIA_PHONE_MESSAGE)
        String phoneNumber,

        Gender gender
) {
}
