package org.group1.coffeeshopapi.common.excel;

import org.group1.coffeeshopapi.common.enums.ReportPeriod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Wording shared by the Excel banners: dates, periods, money and names written the same way everywhere. */
public final class ExcelText {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private ExcelText() {
    }

    /** "9 Nov 2026". */
    public static String date(LocalDate date) {
        return date == null ? "-" : DATE.format(date);
    }

    /** "November 2026". */
    public static String month(YearMonth month) {
        return MONTH.format(month);
    }

    /** "Daily, 9 Nov 2026", "Monthly, 1 Nov 2026 to 30 Nov 2026". */
    public static String period(ReportPeriod period, LocalDate from, LocalDate to) {
        String name = switch (period) {
            case DAY -> "Daily";
            case MONTH -> "Monthly";
            case YEAR -> "Yearly";
        };
        return from.equals(to) ? name + ", " + date(from) : name + ", " + date(from) + " to " + date(to);
    }

    /** "$1,234.50". */
    public static String usd(BigDecimal amount) {
        return amount == null ? "-" : String.format(Locale.ENGLISH, "$%,.2f", amount.setScale(2, RoundingMode.HALF_UP));
    }

    /** "sophal" → "Sophal", the capitalisation the admin pages show. */
    public static String displayName(String name) {
        if (name == null || name.isBlank()) {
            return "Unknown";
        }
        StringBuilder out = new StringBuilder(name.length());
        boolean wordStart = true;
        for (char c : name.toCharArray()) {
            out.append(wordStart ? Character.toUpperCase(c) : c);
            wordStart = Character.isWhitespace(c);
        }
        return out.toString();
    }

    /** "EXPIRED" → "Expired", "OVER_PRODUCED" → "Over produced". */
    public static String readable(Enum<?> value) {
        String words = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
