package org.group1.coffeeshopapi.admin.service.impl;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.admin.entity.Inventory;
import org.group1.coffeeshopapi.admin.entity.Product;
import org.group1.coffeeshopapi.admin.dto.request.InventoryPatchRequest;
import org.group1.coffeeshopapi.admin.dto.request.InventoryRequest;
import org.group1.coffeeshopapi.admin.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.admin.mapper.InventoryMapper;
import org.group1.coffeeshopapi.admin.repository.InventoryRepository;
import org.group1.coffeeshopapi.admin.service.InventoryService;
import org.group1.coffeeshopapi.admin.repository.ProductRepository;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    @Transactional
    public InventoryResponse create(InventoryRequest request) {
        if (inventoryRepository.existsByProductId(request.getProductId())) {
            throw new DuplicateResourceException("Inventory already exists for product: " + request.getProductId());
        }
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));
        Inventory inventory = inventoryMapper.toEntity(request, product);
        Inventory saved = inventoryRepository.save(inventory);
        return inventoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public InventoryResponse update(UUID id, InventoryRequest request) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + id));
        inventoryMapper.updateEntity(inventory, request);
        Inventory updated = inventoryRepository.save(inventory);
        return inventoryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public InventoryResponse patch(UUID id, InventoryPatchRequest request) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + id));
        inventoryMapper.patch(inventory, request);
        Inventory updated = inventoryRepository.save(inventory);
        return inventoryMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + id));
        inventoryRepository.delete(inventory);
    }

    @Override
    public InventoryResponse getById(UUID id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found: " + id));
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public InventoryResponse getByProductId(UUID productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public Page<InventoryResponse> getAll(Pageable pageable) {
        return inventoryRepository.findAll(pageable)
                .map(inventoryMapper::toResponse);
    }
}