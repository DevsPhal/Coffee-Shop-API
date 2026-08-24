package org.group1.coffeeshopapi.telegram.service;

public interface TelegramCatalogService {

    /** @param categoryName blank/null for the full menu, else filtered to that category (case-insensitive). */
    String buildMenu(String categoryName);

    String buildCategoryList();

    String buildDiscounts();
}
