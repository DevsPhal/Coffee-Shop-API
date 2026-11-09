package org.group1.coffeeshopapi.common.excel;

/** One labelled figure on a summary sheet, e.g. "Money in  $68.55". */
public record KeyValue(String label, Object value, TableColumn.Kind kind) {

    public static KeyValue text(String label, Object value) {
        return new KeyValue(label, value, TableColumn.Kind.TEXT);
    }

    public static KeyValue count(String label, Number value) {
        return new KeyValue(label, value, TableColumn.Kind.INTEGER);
    }

    public static KeyValue money(String label, Object value) {
        return new KeyValue(label, value, TableColumn.Kind.MONEY);
    }
}
