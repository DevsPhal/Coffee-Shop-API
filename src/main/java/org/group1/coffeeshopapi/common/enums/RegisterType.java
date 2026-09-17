package org.group1.coffeeshopapi.common.enums;

// How an account was created: EMAIL means it has its own email/password (even if Telegram was
// linked later). TELEGRAM means it was created straight from Telegram and has no password —
// login is Telegram-only.
public enum RegisterType {
    EMAIL,
    TELEGRAM
}
