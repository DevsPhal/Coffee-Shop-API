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
}
