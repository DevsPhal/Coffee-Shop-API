package org.group1.coffeeshopapi.telegram.command;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.group1.coffeeshopapi.telegram.service.TelegramCatalogService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoriesCommand implements TelegramCommand {
    private final TelegramCatalogService catalogService;

    @Override
    public String name() {
        return "/categories";
    }

    @Override
    public String execute(TelegramMessage message, String argument) {
        return catalogService.buildCategoryList();
    }

    @Override
    public boolean useHtml() {
        return true;
    }
}
