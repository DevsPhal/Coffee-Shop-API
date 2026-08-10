package org.group1.coffeeshopapi.products.config;

import java.math.BigDecimal;

import org.group1.coffeeshopapi.products.dto.request.ProductCreateRequest;
import org.group1.coffeeshopapi.products.repository.ProductRepository;
import org.group1.coffeeshopapi.products.service.ProductService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ProductDataInitializer {

    @Bean
    @Order(2)
    public CommandLineRunner initializeProductData(ProductService productService, ProductRepository productRepository) {
        return args -> {
            // Only initialize if products table is empty
            if (productRepository.count() == 0) {
                // Noodles
                saveProduct(productService, "D001", "មីកំប៉ុង", "mei", "pcs", "4000", "1.00");
                saveProduct(productService, "D002", "មីគោក ១កញ្ចប់", "mei", "pcs", "2500", "0.65");
                saveProduct(productService, "D003", "មីគោក ២កញ្ចប់", "mei", "pcs", "5000", "1.25");
                saveProduct(productService, "D004", "មីគោក ១ឈុត", "mei", "pcs", "6000", "1.50");
                saveProduct(productService, "D005", "ពងមាន់ចៀន់", "mei", "pcs", "1000", "15.00");
                saveProduct(productService, "D006", "មីសួរ កញ្ចប់", "mei", "pcs", "6000", "1.50");

                // Iced Coffee
                saveProduct(productService, "D007", "ESPRESSO", "Iced", "pcs", "5000", "1.25");
                saveProduct(productService, "D008", "Ice Amacano", "Iced", "pcs", "5000", "1.25");
                saveProduct(productService, "D009", "Ice Latte", "Iced", "pcs", "6000", "1.50");
                saveProduct(productService, "D010", "590 Coffee", "Iced", "pcs", "6000", "1.50");
                saveProduct(productService, "D011", "កាហ្វេ ទឹកដោះ", "Iced", "pcs", "6000", "1.50");
                saveProduct(productService, "D012", "ផាសិនស្រស់", "Iced", "pcs", "6000", "1.50");
                saveProduct(productService, "D013", "ផាសិន ទឹកដោះគោ", "Iced", "pcs", "6000", "1.50");
                saveProduct(productService, "D014", "ផាសិន សូដា", "Iced", "pcs", "6000", "1.50");
                saveProduct(productService, "D015", "សុកូឡា ក្រឡុក", "Iced", "pcs", "8000", "2.00");
                saveProduct(productService, "D020", "តែក្រូចឆ្មា", "Iced", "pcs", "4000", "1.00");
                saveProduct(productService, "D021", "តែក្រហម ទឹកដោះគោ", "Iced", "pcs", "6000", "1.50");
                saveProduct(productService, "D022", "តែបែតង ទឹកដោះគោ", "Iced", "pcs", "6000", "1.50");
                saveProduct(productService, "D023", "កាហ្វេហ្វីន ទឹកកក", "Iced", "pcs", "4000", "1.00");
                saveProduct(productService, "D024", "កាហ្វេហ្វីន ទឹកដោះគោ", "Iced", "pcs", "5000", "1.25");
                saveProduct(productService, "D025", "Blue Soda", "Iced", "pcs", "5000", "1.25");
                saveProduct(productService, "D026", "Strawberry Soda", "Iced", "pcs", "5000", "1.25");

                // Hot Coffee
                saveProduct(productService, "D016", "សុកូឡា ក្តៅ", "hot", "pcs", "6000", "1.50");
                saveProduct(productService, "D017", "ESPRESSO ក្តៅ", "hot", "pcs", "5000", "1.25");
                saveProduct(productService, "D018", "Latte ក្តៅ", "hot", "pcs", "5000", "1.25");
                saveProduct(productService, "D019", "តែបែតង ក្តៅ", "hot", "pcs", "5000", "1.25");

                // Beer
                saveProduct(productService, "D027", "Carlsberg Beer ដប", "beer", "pcs", "5000", "1.25");
                saveProduct(productService, "D028", "Angkor Sky", "beer", "pcs", "4000", "1.00");
                saveProduct(productService, "D029", "Cambodia Beer កំប៉ុង", "beer", "pcs", "3500", "0.85");

                // Soft Drinks
                saveProduct(productService, "D030", "ទឹកកក ធុងតូច", "softdrink", "pcs", "2500", "0.65");
                saveProduct(productService, "D031", "ទឹកកក ធុងធំ", "softdrink", "pcs", "5000", "1.25");
                saveProduct(productService, "D032", "ទឹកកក កែវតូច", "softdrink", "pcs", "1500", "0.45");
                saveProduct(productService, "D033", "ទឹកកក កែវធំ", "softdrink", "pcs", "1000", "0.25");
                saveProduct(productService, "D034", "Sting លឿង", "softdrink", "pcs", "4000", "1.00");
                saveProduct(productService, "D035", "Sting ក្រហម", "softdrink", "pcs", "4000", "1.00");
                saveProduct(productService, "D036", "Sting ស្វាយ", "softdrink", "pcs", "4000", "1.00");
                saveProduct(productService, "D037", "គោជុល", "softdrink", "pcs", "5000", "1.25");
                saveProduct(productService, "D038", "បាកាស", "softdrink", "pcs", "5000", "1.25");
                saveProduct(productService, "D039", "Pepei កំប៉ុង", "softdrink", "pcs", "4000", "1.00");
                saveProduct(productService, "D040", "Coca Cola កំប៉ុង", "softdrink", "pcs", "4000", "1.00");
                saveProduct(productService, "D041", "ទឹក Hi-Tech ដបធំ", "softdrink", "pcs", "2500", "0.65");
                saveProduct(productService, "D042", "ទឹក Hi-Tech ដបតូច", "softdrink", "pcs", "1500", "0.45");
                saveProduct(productService, "D043", "ទឹក Angkor ដបធំ", "softdrink", "pcs", "2500", "0.65");
                saveProduct(productService, "D044", "ទឹក Angkor ដបតូច", "softdrink", "pcs", "1500", "0.45");

                System.out.println("Product data initialized successfully!");
            }
        };
    }

    private void saveProduct(ProductService productService, String productId, String name,
                            String categoryCode, String unitCode, String priceRiel, String priceDollar) {
        try {
            ProductCreateRequest request = new ProductCreateRequest();
            request.setProductId(productId);
            request.setName(name);
            request.setCategoryCode(categoryCode);
            request.setUnitCode(unitCode);
            request.setPriceRiel(new BigDecimal(priceRiel));
            request.setPriceDollar(new BigDecimal(priceDollar));
            request.setIsActive(true);
            
            productService.createProduct(request);
        } catch (Exception e) {
            System.out.println("Error creating product " + productId + ": " + e.getMessage());
        }
    }
}
