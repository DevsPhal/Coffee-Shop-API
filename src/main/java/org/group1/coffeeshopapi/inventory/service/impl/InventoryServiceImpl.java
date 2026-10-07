package org.group1.coffeeshopapi.inventory.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.inventory.dto.request.StockCutRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockInRequest;
import org.group1.coffeeshopapi.inventory.dto.response.BatchConsumptionResponse;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.inventory.dto.response.StockCutResponse;
import org.group1.coffeeshopapi.inventory.dto.response.StockMovementResponse;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.entity.StockBatch;
import org.group1.coffeeshopapi.inventory.entity.StockExpense;
import org.group1.coffeeshopapi.inventory.entity.StockMovement;
import org.group1.coffeeshopapi.common.enums.StockMovementType;
import org.group1.coffeeshopapi.common.enums.StockStrategy;
import org.group1.coffeeshopapi.inventory.mapper.InventoryMapper;
import org.group1.coffeeshopapi.inventory.mapper.StockMovementMapper;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.inventory.repository.StockBatchRepository;
import org.group1.coffeeshopapi.inventory.repository.StockExpenseRepository;
import org.group1.coffeeshopapi.inventory.repository.StockMovementRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockExpenseRepository stockExpenseRepository;
    private final ProductRepository productRepository;
    private final InventoryMapper inventoryMapper;
    private final StockMovementMapper stockMovementMapper;
    private final ActorLookupService actorLookupService;

    @Override
    public InventoryResponse getByProduct(UUID productId) {
        return inventoryMapper.toResponse(findInventory(productId));
    }

    @Override
    public Page<InventoryResponse> list(Pageable pageable) {
        return inventoryRepository.findAll(pageable).map(inventoryMapper::toResponse);
    }

    @Override
    public Page<InventoryResponse> listLowStock(Pageable pageable) {
        return inventoryRepository.findLowStock(pageable).map(inventoryMapper::toResponse);
    }

    @Override
    @Transactional
    public StockMovementResponse stockIn(StockInRequest request, UUID performedBy) {
        Inventory inventory = findInventoryForUpdate(request.productId());
        Product product = inventory.getProduct();

        StockBatch batch = new StockBatch();
        batch.setProduct(product);
        batch.setQuantity(request.quantity());
        batch.setRemainingQuantity(request.quantity());
        batch.setUnitCost(request.unitCost());
        stockBatchRepository.save(batch);

        inventory.setQuantityOnHand(inventory.getQuantityOnHand().add(request.quantity()));
        inventoryRepository.save(inventory);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setType(StockMovementType.STOCK_IN);
        movement.setQuantity(request.quantity());
        movement.setNote(request.note());
        movement.setPerformedBy(performedBy);
        stockMovementRepository.save(movement);

        recordStockPurchaseExpense(product, movement, request.quantity(), request.unitCost());

        return toResponse(movement);
    }

    private void recordStockPurchaseExpense(Product product, StockMovement movement, BigDecimal quantity, BigDecimal unitCost) {
        StockExpense expense = new StockExpense();
        expense.setProduct(product);
        expense.setStockMovement(movement);
        expense.setQuantity(quantity);
        expense.setUnitCost(unitCost);
        expense.setAmount(quantity.multiply(unitCost));
        expense.setExpenseDate(LocalDate.now());
        stockExpenseRepository.save(expense);
    }

    @Override
    @Transactional
    public StockCutResponse stockCut(StockCutRequest request, UUID performedBy) {
        Inventory inventory = findInventoryForUpdate(request.productId());
        if (inventory.getQuantityOnHand().compareTo(request.quantity()) < 0) {
            throw new InvalidOperationException(
                    "Insufficient stock: available " + inventory.getQuantityOnHand() + ", requested " + request.quantity());
        }
        return cut(inventory, request, request.quantity(), performedBy);
    }

    @Override
    @Transactional
    public StockCutResponse stockCutAvailable(StockCutRequest request, UUID performedBy) {
        Inventory inventory = findInventoryForUpdate(request.productId());
        BigDecimal quantity = request.quantity().min(inventory.getQuantityOnHand());
        if (quantity.compareTo(request.quantity()) < 0) {
            log.warn("Stock short for product {}: sold {}, only {} on hand — cutting what's there",
                    request.productId(), request.quantity(), inventory.getQuantityOnHand());
        }
        if (quantity.signum() <= 0) {
            return null;
        }
        return cut(inventory, request, quantity, performedBy);
    }

    @Override
    public void requireAvailable(UUID productId, BigDecimal quantity) {
        Inventory inventory = findInventory(productId);
        if (inventory.getQuantityOnHand().compareTo(quantity) < 0) {
            throw new InvalidOperationException("'" + inventory.getProduct().getName()
                    + "' doesn't have enough stock left for this order");
        }
    }

    private StockCutResponse cut(Inventory inventory, StockCutRequest request, BigDecimal requestedQuantity,
            UUID performedBy) {
        Product product = inventory.getProduct();

        List<StockBatch> batches = request.strategy() == StockStrategy.FIFO
                ? stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(
                        product.getId(), BigDecimal.ZERO)
                : stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtDesc(
                        product.getId(), BigDecimal.ZERO);

        List<BatchConsumptionResponse> consumptions = new ArrayList<>();
        List<StockBatch> touchedBatches = new ArrayList<>();
        BigDecimal remainingToCut = requestedQuantity;

        for (StockBatch batch : batches) {
            if (remainingToCut.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal takeFromBatch = batch.getRemainingQuantity().min(remainingToCut);
            batch.setRemainingQuantity(batch.getRemainingQuantity().subtract(takeFromBatch));
            remainingToCut = remainingToCut.subtract(takeFromBatch);

            touchedBatches.add(batch);
            consumptions.add(new BatchConsumptionResponse(
                    batch.getId(), batch.getCreatedAt(), takeFromBatch, batch.getUnitCost()));
        }

        if (remainingToCut.compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidOperationException("Insufficient batch stock to fulfill this cut");
        }

        stockBatchRepository.saveAll(touchedBatches);

        inventory.setQuantityOnHand(inventory.getQuantityOnHand().subtract(requestedQuantity));
        inventoryRepository.save(inventory);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setType(StockMovementType.STOCK_OUT);
        movement.setStrategy(request.strategy());
        movement.setQuantity(requestedQuantity);
        movement.setNote(request.note());
        movement.setPerformedBy(performedBy);
        stockMovementRepository.save(movement);

        return new StockCutResponse(
                movement.getId(),
                product.getId(),
                request.strategy(),
                requestedQuantity,
                inventory.getQuantityOnHand(),
                consumptions);
    }

    @Override
    public Page<StockMovementResponse> listMovements(UUID productId, Pageable pageable) {
        Page<StockMovement> movements = stockMovementRepository.findByProductId(productId, pageable);

        Set<UUID> actorIds = new HashSet<>();
        for (StockMovement movement : movements) {
            actorIds.add(movement.getPerformedBy());
        }
        Map<UUID, ActorSummary> actors = actorLookupService.resolveAll(actorIds);

        return movements.map(movement -> stockMovementMapper.toResponse(movement, actors.get(movement.getPerformedBy())));
    }

    private StockMovementResponse toResponse(StockMovement movement) {
        return stockMovementMapper.toResponse(movement, actorLookupService.resolve(movement.getPerformedBy()));
    }

    private Inventory findInventory(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product"));
    }

    private Inventory findInventoryForUpdate(UUID productId) {
        return inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product"));
    }
}
