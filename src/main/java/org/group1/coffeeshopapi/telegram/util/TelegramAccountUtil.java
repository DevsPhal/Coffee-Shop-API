package org.group1.coffeeshopapi.telegram.util;

import java.util.UUID;

// A fake but unique email for accounts created without a real one (Telegram invites/signups).
// The email column is required and doubles as the login username, so it can't be left blank.
public final class TelegramAccountUtil {
    private TelegramAccountUtil() {
    }

    public static String placeholderEmail() {
        return "telegram+" + UUID.randomUUID() + "@telegram.internal";
    }
}
