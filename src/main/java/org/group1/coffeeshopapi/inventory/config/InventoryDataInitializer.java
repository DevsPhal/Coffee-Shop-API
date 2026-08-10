package org.group1.coffeeshopapi.inventory.config;

import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.products.entity.Product;
import org.group1.coffeeshopapi.products.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class InventoryDataInitializer {

    private static final int DEFAULT_STOCK = 50;
    private static final int DEFAULT_LOW_STOCK_THRESHOLD = 10;

    @Bean
    @Order(3)
    public CommandLineRunner initializeInventoryData(ProductRepository productRepository, InventoryRepository inventoryRepository) {
        return args -> {
            if (inventoryRepository.count() == 0) {
                for (Product product : productRepository.findAll()) {
                    Inventory inventory = new Inventory();
                    inventory.setProduct(product);
                    inventory.setQuantityOnHand(DEFAULT_STOCK);
                    inventory.setLowStockThreshold(DEFAULT_LOW_STOCK_THRESHOLD);
                    inventoryRepository.save(inventory);
                }
                System.out.println("Inventory data initialized successfully!");
            }
        };
    }
}
