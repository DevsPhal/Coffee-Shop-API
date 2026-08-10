package org.group1.coffeeshopapi.categories.config;

import org.group1.coffeeshopapi.categories.dto.request.CategoryCreateRequest;
import org.group1.coffeeshopapi.categories.repository.CategoryRepository;
import org.group1.coffeeshopapi.categories.service.CategoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class CategoryDataInitializer {

    @Bean
    @Order(1)
    public CommandLineRunner initializeCategoryData(CategoryService categoryService, CategoryRepository categoryRepository) {
        return args -> {
            if (categoryRepository.count() == 0) {
                saveCategory(categoryService, "mei", "Noodles & Snacks", "Instant noodles and quick bites");
                saveCategory(categoryService, "Iced", "Iced Drinks", "Iced coffee, tea and cold beverages");
                saveCategory(categoryService, "hot", "Hot Drinks", "Hot coffee and tea");
                saveCategory(categoryService, "beer", "Beer", "Bottled and canned beer");
                saveCategory(categoryService, "softdrink", "Soft Drinks & Water", "Ice, soda, energy drinks and bottled water");

                System.out.println("Category data initialized successfully!");
            }
        };
    }

    private void saveCategory(CategoryService categoryService, String code, String name, String description) {
        try {
            CategoryCreateRequest request = new CategoryCreateRequest();
            request.setCode(code);
            request.setName(name);
            request.setDescription(description);
            request.setIsActive(true);

            categoryService.createCategory(request);
        } catch (Exception e) {
            System.out.println("Error creating category " + code + ": " + e.getMessage());
        }
    }
}
