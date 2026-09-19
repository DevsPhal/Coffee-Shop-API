package org.group1.coffeeshopapi.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;
import org.group1.coffeeshopapi.common.enums.Gender;

// Fills in what Telegram widget login doesn't collect. Phone number is required here (unlike
// UpdateProfileRequest, where it's optional), since Telegram never provides one. Gender stays
// optional, same as everywhere else.
public record CompleteProfileRequest(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = ValidationPatterns.CAMBODIA_PHONE_REGEX, message = ValidationPatterns.CAMBODIA_PHONE_MESSAGE)
        @Schema(example = "072 345 5674")
        String phoneNumber,

        Gender gender
) {
}
