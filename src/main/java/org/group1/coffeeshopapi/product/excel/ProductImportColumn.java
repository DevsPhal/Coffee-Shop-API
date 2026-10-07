package org.group1.coffeeshopapi.product.excel;

import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.common.excel.ExcelColumn;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public enum ProductImportColumn implements ExcelColumn {
    NAME("Name", true, 0, Set.of("productname"), 26,
            "Text, up to 255 characters",
            "Product name shown on the menu, receipts and reports.",
            "Iced Latte"),
    NAME_KH("Name (Khmer)", false, 10, Set.of("namekh", "khmername"), 22,
            "Text, up to 255 characters",
            "Optional Khmer name shown to Khmer-language customers.",
            "ឡាតេទឹកកក"),
    DESCRIPTION("Description", false, 1, Set.of(), 34,
            "Text, up to 255 characters",
            "Optional short description shown on the product page.",
            "Espresso with cold milk over ice"),
    CATEGORY("Category", true, 5, Set.of("categoryname"), 20,
            "Exact name of an existing category (pick from the dropdown / Categories sheet)",
            "The category decides the SKU prefix and which variants are allowed: drink categories "
                    + "(FRESH_DRINK, BEVERAGE) sell in MEDIUM/LARGE sizes, snack and other categories sell per PIECE.",
            "Coffee"),
    SKU("SKU", false, 2, Set.of("productcode", "code"), 20,
            "Leave blank to auto-generate, or type your own: letters, digits and - _ . (max 64)",
            "Unique stock-keeping code used to find the product in the stock-in import. Blank = generated from the "
                    + "category and name as GROUP-CATEGORY-NAME-### (e.g. FD-COF-IL-001). A typed value is used "
                    + "as-is in upper case. Variant SKUs automatically add -M (MEDIUM), -L (LARGE) or -PC (PIECE).",
            "FD-COF-IL-001"),
    PRICE("Price", true, 4, Set.of("mediumprice", "baseprice", "unitprice"), 12,
            "Number >= 0 in USD",
            "Selling price. For drink categories this is the MEDIUM size price; for snack and other categories "
                    + "it is the price per PIECE.",
            "2.50"),
    LARGE_PRICE("Large Price", false, -1, Set.of("pricelarge"), 12,
            "Number >= 0 in USD — drink categories only",
            "Optional LARGE size price. Fill it only for drinks sold in two sizes; leave blank for snacks "
                    + "or drinks sold in one size.",
            "3.00"),
    STOCK_UNIT("Stock Unit", true, 3, Set.of("unit"), 14,
            "One of: " + names(StockUnit.values()),
            "The unit you buy and count stock in (used by stock-in and the low-stock report).",
            "PACK"),
    SELL_UNIT("Sell Unit", false, 8, Set.of(), 14,
            "One of: " + names(SellUnit.values()) + ". Blank = CUP for drinks, PIECE for others",
            "The unit a customer buys.",
            "CUP"),
    UNITS_PER_STOCK("Units Per Stock", false, 9, Set.of("unitsperstockunit"), 16,
            "Number > 0. Blank = 1",
            "How many sell units come out of one stock unit. Example: 1 PACK of beans makes 40 CUPs, so enter 40 "
                    + "and each cup sold deducts 1/40 of a PACK.",
            "40"),
    REORDER_LEVEL("Reorder Level", false, 6, Set.of("reorder", "minstock"), 14,
            "Number >= 0 in stock units. Blank = 0",
            "When stock on hand falls to this level the product appears in the low-stock report.",
            "5"),
    VARIANTS("Variants", false, 7, Set.of(), 30,
            "NAME:PRICE;NAME:PRICE using MEDIUM, LARGE or PIECE",
            "Legacy alternative to Price / Large Price. When filled it replaces them.",
            "MEDIUM:2.50;LARGE:3.00");

    private final String header;
    private final boolean required;
    private final int legacyIndex;
    private final Set<String> aliases;
    private final int width;
    private final String format;
    private final String description;
    private final String example;

    ProductImportColumn(String header, boolean required, int legacyIndex, Set<String> aliases, int width,
            String format, String description, String example) {
        this.header = header;
        this.required = required;
        this.legacyIndex = legacyIndex;
        this.aliases = aliases;
        this.width = width;
        this.format = format;
        this.description = description;
        this.example = example;
    }

    public static List<ProductImportColumn> templateColumns() {
        return Arrays.stream(values()).filter(column -> column != VARIANTS).toList();
    }

    public int templateIndex() {
        return templateColumns().indexOf(this);
    }

    private static String names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.joining(", "));
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
        return true;
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
