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

    public static String wholeAmount(BigDecimal amount, String currencyCode) {
        return amount.setScale(0, RoundingMode.HALF_UP) + " " + currencyCode;
    }

    public static String escape(String text) {
        return HtmlUtils.htmlEscape(text);
    }

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
