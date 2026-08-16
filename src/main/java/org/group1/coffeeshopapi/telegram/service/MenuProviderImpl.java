package org.group1.coffeeshopapi.telegram.service;

import org.group1.coffeeshopapi.products.repository.ProductRepository;
import org.group1.coffeeshopapi.telegram.command.MenuCommand;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MenuProviderImpl implements MenuCommand.MenuProvider {

    private final ProductRepository productRepository;

    public MenuProviderImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<String> availableItems() {
        return productRepository.findByIsActiveTrue().stream()
                .map(p -> p.getName() + " — $" + p.getPriceDollar())
                .collect(Collectors.toList());
    }
}
