package org.group1.coffeeshopapi.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;
import org.group1.coffeeshopapi.common.enums.Gender;

// Profile edit — every field is optional, and a null one is left unchanged. Used both for a
// user editing their own profile and for an admin editing someone else's by id. Email and
// password change through their own dedicated flows, so they're not here.
public record UpdateProfileRequest(
        @Schema(example = "Sophal Nem")
        String fullName,

        @Pattern(regexp = ValidationPatterns.CAMBODIA_PHONE_REGEX, message = ValidationPatterns.CAMBODIA_PHONE_MESSAGE)
        @Schema(example = "072 345 5674")
        String phoneNumber,

        Gender gender
) {
}
