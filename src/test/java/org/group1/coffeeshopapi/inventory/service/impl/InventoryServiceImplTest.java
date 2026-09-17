package org.group1.coffeeshopapi.inventory.service.impl;

import org.group1.coffeeshopapi.common.enums.StockMovementType;
import org.group1.coffeeshopapi.common.enums.StockStrategy;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.inventory.dto.request.StockCutRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockInRequest;
import org.group1.coffeeshopapi.inventory.dto.response.StockCutResponse;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.entity.StockBatch;
import org.group1.coffeeshopapi.inventory.entity.StockExpense;
import org.group1.coffeeshopapi.inventory.entity.StockMovement;
import org.group1.coffeeshopapi.inventory.mapper.InventoryMapper;
import org.group1.coffeeshopapi.inventory.mapper.StockMovementMapper;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.inventory.repository.StockBatchRepository;
import org.group1.coffeeshopapi.inventory.repository.StockExpenseRepository;
import org.group1.coffeeshopapi.inventory.repository.StockMovementRepository;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the FIFO/LIFO stock-cut algorithm and stock-in bookkeeping — the core of the inventory
 * module and, until now, entirely untested. See InventoryServiceImpl#stockCut/stockIn.
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private StockBatchRepository stockBatchRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private StockExpenseRepository stockExpenseRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryMapper inventoryMapper;
    @Mock private StockMovementMapper stockMovementMapper;
    @Mock private ActorLookupService actorLookupService;
    @InjectMocks private InventoryServiceImpl service;

    @Test
    void cuttingStockFifoConsumesTheOldestBatchFirstThenSpillsIntoTheNext() {
        Product product = product();
        Inventory inventory = inventoryWithQuantity(product, new BigDecimal("15"));
        StockBatch oldest = batch(new BigDecimal("10"), new BigDecimal("2.00"));
        StockBatch newer = batch(new BigDecimal("10"), new BigDecimal("3.00"));

        when(inventoryRepository.findByProductIdForUpdate(product.getId())).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(
                product.getId(), BigDecimal.ZERO)).thenReturn(List.of(oldest, newer));

        UUID actorId = UUID.randomUUID();
        StockCutResponse response = service.stockCut(
                new StockCutRequest(product.getId(), new BigDecimal("12"), StockStrategy.FIFO, "Sold"), actorId);

        assertThat(oldest.getRemainingQuantity()).isEqualByComparingTo("0");
        assertThat(newer.getRemainingQuantity()).isEqualByComparingTo("8");
        assertThat(inventory.getQuantityOnHand()).isEqualByComparingTo("3");
        assertThat(response.consumptions()).hasSize(2);
        assertThat(response.consumptions().get(0).quantityTaken()).isEqualByComparingTo("10");
        assertThat(response.consumptions().get(1).quantityTaken()).isEqualByComparingTo("2");
    }

    @Test
    void cuttingStockLifoConsumesTheNewestBatchFirst() {
        Product product = product();
        Inventory inventory = inventoryWithQuantity(product, new BigDecimal("15"));
        StockBatch newest = batch(new BigDecimal("10"), new BigDecimal("3.00"));

        when(inventoryRepository.findByProductIdForUpdate(product.getId())).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtDesc(
                product.getId(), BigDecimal.ZERO)).thenReturn(List.of(newest));

        service.stockCut(new StockCutRequest(product.getId(), new BigDecimal("6"), StockStrategy.LIFO, null),
                UUID.randomUUID());

        assertThat(newest.getRemainingQuantity()).isEqualByComparingTo("4");
        assertThat(inventory.getQuantityOnHand()).isEqualByComparingTo("9");
    }

    @Test
    void cuttingMoreStockThanIsOnHandIsRejectedBeforeTouchingAnyBatch() {
        Product product = product();
        Inventory inventory = inventoryWithQuantity(product, new BigDecimal("5"));
        when(inventoryRepository.findByProductIdForUpdate(product.getId())).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> service.stockCut(
                new StockCutRequest(product.getId(), new BigDecimal("10"), StockStrategy.FIFO, null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Insufficient stock");

        verify(stockBatchRepository, never()).saveAll(any());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void cuttingStockWhenBatchesHaveDriftedBelowQuantityOnHandIsRejected() {
        // quantityOnHand says there's enough, but the batches backing it don't actually cover it —
        // a data-drift scenario that must fail loudly rather than silently under-cut.
        Product product = product();
        Inventory inventory = inventoryWithQuantity(product, new BigDecimal("10"));
        StockBatch onlyBatch = batch(new BigDecimal("4"), new BigDecimal("2.00"));

        when(inventoryRepository.findByProductIdForUpdate(product.getId())).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(
                product.getId(), BigDecimal.ZERO)).thenReturn(List.of(onlyBatch));

        assertThatThrownBy(() -> service.stockCut(
                new StockCutRequest(product.getId(), new BigDecimal("10"), StockStrategy.FIFO, null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Insufficient batch stock");
    }

    @Test
    void stockingInAddsANewBatchIncreasesOnHandQuantityAndRecordsAPurchaseExpense() {
        Product product = product();
        Inventory inventory = inventoryWithQuantity(product, new BigDecimal("5"));
        when(inventoryRepository.findByProductIdForUpdate(product.getId())).thenReturn(Optional.of(inventory));
        when(stockMovementMapper.toResponse(any(StockMovement.class), any())).thenReturn(null);

        service.stockIn(new StockInRequest(product.getId(), new BigDecimal("20"), new BigDecimal("1.50"), "Delivery"),
                UUID.randomUUID());

        assertThat(inventory.getQuantityOnHand()).isEqualByComparingTo("25");

        ArgumentCaptor<StockBatch> batchCaptor = ArgumentCaptor.forClass(StockBatch.class);
        verify(stockBatchRepository).save(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getRemainingQuantity()).isEqualByComparingTo("20");

        ArgumentCaptor<StockExpense> expenseCaptor = ArgumentCaptor.forClass(StockExpense.class);
        verify(stockExpenseRepository).save(expenseCaptor.capture());
        assertThat(expenseCaptor.getValue().getAmount()).isEqualByComparingTo("30.00");

        ArgumentCaptor<StockMovement> movementCaptor = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getType()).isEqualTo(StockMovementType.STOCK_IN);
    }

    private Product product() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Green Tea");
        return product;
    }

    private Inventory inventoryWithQuantity(Product product, BigDecimal quantity) {
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setQuantityOnHand(quantity);
        return inventory;
    }

    private StockBatch batch(BigDecimal remaining, BigDecimal unitCost) {
        StockBatch batch = new StockBatch();
        batch.setRemainingQuantity(remaining);
        batch.setUnitCost(unitCost);
        return batch;
    }
}
