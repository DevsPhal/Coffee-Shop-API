package org.group1.coffeeshopapi.telegram.util;

import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TelegramFormat {

    private TelegramFormat() {
    }

    public static String usd(BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.HALF_UP);
    }

    /** Whole-number currencies (e.g. KHR) have no decimal places to show. */
    public static String wholeAmount(BigDecimal amount, String currencyCode) {
        return amount.setScale(0, RoundingMode.HALF_UP) + " " + currencyCode;
    }

    /** Escapes admin/user-entered text before it's interpolated into an HTML-parse-mode message. */
    public static String escape(String text) {
        return HtmlUtils.htmlEscape(text);
    }

    /**
     * Title-cases a name (e.g. a product, category, or event name) for display — "iced latte"
     * becomes "Iced Latte" — regardless of how it was typed when created. Call this before
     * {@link #escape(String)}.
     */
    public static String titleCase(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String[] words = text.trim().toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(' ');
            }
            String word = words[i];
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    /**
     * Normalizes free-text admin content (e.g. an event description) into a professional-looking
     * sentence: capitalizes the first letter, collapses stray whitespace, and ensures it ends with
     * terminal punctuation. Call this before {@link #escape(String)}.
     */
    public static String professionalize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        String capitalized = Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
        char last = capitalized.charAt(capitalized.length() - 1);
        return (last == '.' || last == '!' || last == '?') ? capitalized : capitalized + ".";
    }
}
