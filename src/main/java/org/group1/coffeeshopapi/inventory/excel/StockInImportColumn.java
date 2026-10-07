package org.group1.coffeeshopapi.inventory.excel;

import org.group1.coffeeshopapi.common.excel.ExcelColumn;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public enum StockInImportColumn implements ExcelColumn {
    SKU("SKU", true, true, 0, Set.of("productsku", "productcode", "code"), 20,
            "SKU of an existing product (pick from the dropdown / Products sheet)",
            "Identifies the product receiving stock. Stock is tracked per product, so a variant SKU such as "
                    + "FD-COF-IL-001-M is accepted and counted on its product FD-COF-IL-001.",
            "FD-COF-IL-001"),
    PRODUCT("Product", false, false, -1, Set.of("productname", "name"), 30,
            "Filled automatically from the SKU — do not type here",
            "Shows the product name and its stock unit so you can check you picked the right SKU. Ignored on import.",
            "Iced Latte (PACK)"),
    QUANTITY("Quantity", true, true, 1, Set.of("qty", "quantityreceived"), 12,
            "Number > 0, in the product's Stock Unit",
            "How much stock you received, counted in the product's stock unit (see the Products sheet). "
                    + "Example: 3 means 3 PACKs for a product whose stock unit is PACK.",
            "3"),
    UNIT_COST("Unit Cost", true, true, 2, Set.of("cost", "costperunit", "purchaseprice"), 12,
            "Number >= 0 in USD, per stock unit",
            "Purchase cost of ONE stock unit. Quantity x Unit Cost is recorded as a stock expense and used "
                    + "for FIFO costing.",
            "12.50"),
    NOTE("Note", false, true, 3, Set.of("notes", "remark", "remarks"), 34,
            "Text, optional",
            "Free text saved on the stock movement, e.g. supplier or invoice number.",
            "Supplier ABC, invoice #1024");

    private final String header;
    private final boolean required;
    private final boolean input;
    private final int legacyIndex;
    private final Set<String> aliases;
    private final int width;
    private final String format;
    private final String description;
    private final String example;

    StockInImportColumn(String header, boolean required, boolean input, int legacyIndex, Set<String> aliases,
            int width, String format, String description, String example) {
        this.header = header;
        this.required = required;
        this.input = input;
        this.legacyIndex = legacyIndex;
        this.aliases = aliases;
        this.width = width;
        this.format = format;
        this.description = description;
        this.example = example;
    }

    public static List<StockInImportColumn> templateColumns() {
        return Arrays.asList(values());
    }

    public int templateIndex() {
        return ordinal();
    }

    @Override
    public String header() {
        return header;
    }

    @Override
    public boolean required() {
        return required;
    }

    @Override
    public boolean input() {
        return input;
    }

    @Override
    public int legacyIndex() {
        return legacyIndex;
    }

    @Override
    public Set<String> aliases() {
        return aliases;
    }

    @Override
    public String format() {
        return format;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String example() {
        return example;
    }

    @Override
    public int width() {
        return width;
    }
}
