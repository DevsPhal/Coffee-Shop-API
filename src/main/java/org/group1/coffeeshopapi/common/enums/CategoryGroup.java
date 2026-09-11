package org.group1.coffeeshopapi.common.enums;

// The top-level menu grouping a Category belongs to, e.g. "Iced Coffee" and "Tea" both fall
// under FRESH_DRINK, "Beer" and "Water" under BEVERAGE. Nullable on Category — an internal,
// non-menu category like "Material" (stock-in ingredients) belongs to none of these.
public enum CategoryGroup {
    FRESH_DRINK, BEVERAGE, SNACK
}
