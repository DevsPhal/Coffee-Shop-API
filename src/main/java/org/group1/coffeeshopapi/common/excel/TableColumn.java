package org.group1.coffeeshopapi.common.excel;

/** A column of a branded table: its header and how its values are aligned and formatted. */
public record TableColumn(String header, Kind kind) {

    public enum Kind {
        /** Left-aligned text. */
        TEXT,
        /** Whole numbers with thousands separators, e.g. order counts. */
        INTEGER,
        /** Stock quantities: thousands separators and up to three decimals. */
        QUANTITY,
        /** US dollars, two decimals: $1,234.50. */
        MONEY,
        /** Riel, whole and already rounded to the nearest 100: 41,000 KHR. */
        KHR
    }

    public static TableColumn text(String header) {
        return new TableColumn(header, Kind.TEXT);
    }

    public static TableColumn integer(String header) {
        return new TableColumn(header, Kind.INTEGER);
    }

    public static TableColumn quantity(String header) {
        return new TableColumn(header, Kind.QUANTITY);
    }

    public static TableColumn money(String header) {
        return new TableColumn(header, Kind.MONEY);
    }
}
