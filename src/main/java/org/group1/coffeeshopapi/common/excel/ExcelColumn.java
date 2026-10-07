package org.group1.coffeeshopapi.common.excel;

import java.util.Set;

public interface ExcelColumn {
    String header();

    boolean required();

    boolean input();

    int legacyIndex();

    Set<String> aliases();

    String format();

    String description();

    String example();

    int width();

    default String headerLabel() {
        return required() ? header() + " *" : header();
    }
}
