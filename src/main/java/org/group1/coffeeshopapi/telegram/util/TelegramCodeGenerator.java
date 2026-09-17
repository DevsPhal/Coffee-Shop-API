package org.group1.coffeeshopapi.telegram.util;

import java.security.SecureRandom;

// Generates a one-time code for Telegram account linking. Excludes visually ambiguous characters
// (0/O, 1/I/L) so it's easy to read and type back correctly.
public final class TelegramCodeGenerator {
    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final int LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TelegramCodeGenerator() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
