package org.group1.coffeeshopapi.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.group1.coffeeshopapi.common.constant.ValidationPatterns;
import org.group1.coffeeshopapi.common.enums.Gender;

// Unlike CreateStaffRequest, no email or password is collected: identity is proven by the
// invitee sharing this exact phone number from their own Telegram contact card (see
// TelegramLinkServiceImpl#verifyPendingContact), and login afterward is Telegram-widget-only
// (AuthServiceImpl#loginViaTelegramWidget) — there's no email/password for them to know or use.
public record InviteStaffRequest(
        @NotBlank(message = "Full name is required")
        @Schema(example = "Chanden Sok")
        String fullName,

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = ValidationPatterns.CAMBODIA_PHONE_REGEX, message = ValidationPatterns.CAMBODIA_PHONE_MESSAGE)
        @Schema(example = "072 345 5674")
        String phoneNumber,

        Gender gender
) {
}
