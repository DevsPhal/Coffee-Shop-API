package org.group1.coffeeshopapi.telegram.service;

public interface TelegramCatalogService {

    String buildMenu(String categoryName);

    String buildCategoryList();

    String buildDiscounts();
}
