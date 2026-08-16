package org.group1.coffeeshopapi.pos.service.impl;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.pos.dto.response.PosCatalogItemResponse;
import org.group1.coffeeshopapi.pos.service.PosService;
import org.group1.coffeeshopapi.products.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class PosImpl implements PosService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PosCatalogItemResponse> getProducts() {
        Map<UUID, Inventory> stockByProductId = inventoryRepository.findAll().stream()
                .collect(Collectors.toMap(inv -> inv.getProduct().getId(), Function.identity()));

        return productRepository.findAllActive().stream().map(p -> {
            Inventory stock = stockByProductId.get(p.getId());
            PosCatalogItemResponse item = new PosCatalogItemResponse();
            item.setId(p.getId());
            item.setName(p.getName());
            item.setCategory(p.getCategory() == null ? null : p.getCategory().getName());
            item.setPrice(p.getPriceDollar() == null ? 0D : p.getPriceDollar().doubleValue());
            item.setStock(stock == null ? 0 : stock.getQuantityOnHand());
            item.setUnit(p.getUnitCode() == null ? "pcs" : p.getUnitCode());
            item.setAccent("#3f2b22");
            item.setDiscount(null);
            return item;
        }).collect(Collectors.toList());
    }
}
