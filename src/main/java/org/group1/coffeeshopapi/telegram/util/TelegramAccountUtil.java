package org.group1.coffeeshopapi.telegram.util;

import java.util.UUID;

// Shared by every account created without a real email — a staff invite (StaffServiceImpl
// #buildInvitedStaff) and a customer self-registered via the Telegram Login Widget
// (AuthServiceImpl#registerCustomerViaTelegram) alike. The email column is NOT NULL and doubles
// as the Spring Security username/JWT subject, so every account still needs SOME unique value
// here even though nobody actually has or uses one — never shown to the account holder (see
// UserMapper#toResponse) or usable to log in any other way.
public final class TelegramAccountUtil {
    private TelegramAccountUtil() {
    }

    public static String placeholderEmail() {
        return "telegram+" + UUID.randomUUID() + "@telegram.internal";
    }
}
