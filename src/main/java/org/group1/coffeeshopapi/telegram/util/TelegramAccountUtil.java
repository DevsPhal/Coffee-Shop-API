package org.group1.coffeeshopapi.telegram.util;

import java.util.UUID;

public final class TelegramAccountUtil {
    private TelegramAccountUtil() {
    }

    public static String placeholderEmail() {
        return "telegram+" + UUID.randomUUID() + "@telegram.internal";
    }
}
