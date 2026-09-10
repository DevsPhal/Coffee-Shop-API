package org.group1.coffeeshopapi.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;
import org.group1.coffeeshopapi.common.enums.Gender;

/**
 * Self-service partial update of the signed-in account — any field left {@code null} is left
 * unchanged. Email, role and status are deliberately absent: changing your own email is an auth
 * flow, and moderating your own role or status would let any account escalate itself.
 */
public record UpdateProfileRequest(
        @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
        @Schema(example = "Sok Dara")
        String fullName,

        // An empty string is accepted on top of the usual pattern: null already means "leave
        // alone" here, so clearing a phone number you no longer want on file needs a value of
        // its own. The service turns it back into null before saving.
        @Pattern(regexp = "^$|" + ValidationPatterns.CAMBODIA_PHONE_REGEX,
                message = ValidationPatterns.CAMBODIA_PHONE_MESSAGE)
        @Schema(example = "072 345 5674")
        String phoneNumber,

        Gender gender
) {
}
