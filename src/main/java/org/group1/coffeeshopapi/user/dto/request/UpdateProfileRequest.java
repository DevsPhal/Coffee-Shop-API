package org.group1.coffeeshopapi.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;
import org.group1.coffeeshopapi.common.enums.Gender;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Pattern(regexp = ValidationPatterns.NOT_BLANK_REGEX, message = "Full name can't be blank")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        @Schema(example = "Sophal Nem")
        String fullName,

        @Pattern(regexp = ValidationPatterns.CAMBODIA_PHONE_REGEX, message = ValidationPatterns.CAMBODIA_PHONE_MESSAGE)
        @Schema(example = "072 345 5674")
        String phoneNumber,

        Gender gender
) {
}
