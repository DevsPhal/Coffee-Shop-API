package org.group1.coffeeshopapi.inventory.service.impl;

import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.QuantityUnit;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.StockMovementReason;
import org.group1.coffeeshopapi.common.enums.StockMovementType;
import org.group1.coffeeshopapi.common.enums.StockStrategy;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.common.enums.WasteReason;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.inventory.dto.request.SaleLine;
import org.group1.coffeeshopapi.inventory.dto.request.StockCountRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockCutRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockInRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockReturnRequest;
import org.group1.coffeeshopapi.inventory.dto.request.WasteRequest;
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
import org.group1.coffeeshopapi.inventory.service.VariantStock;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.recipe.entity.RecipeItem;
import org.group1.coffeeshopapi.recipe.repository.RecipeItemRepository;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock private InventoryRepository inventoryRepository;
    @Mock private StockBatchRepository stockBatchRepository;
    @Mock private StockMovementRepository stockMovementRepository;
    @Mock private StockExpenseRepository stockExpenseRepository;
    @Mock private RecipeItemRepository recipeItemRepository;
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
                new StockCutRequest(product.getId(), new BigDecimal("12"), QuantityUnit.BASE, StockStrategy.FIFO, "Sold"), actorId);

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

        service.stockCut(new StockCutRequest(product.getId(), new BigDecimal("6"), QuantityUnit.BASE, StockStrategy.LIFO, null),
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
                new StockCutRequest(product.getId(), new BigDecimal("10"), QuantityUnit.BASE, StockStrategy.FIFO, null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Insufficient stock");

        verify(stockBatchRepository, never()).saveAll(any());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void cuttingStockWhenBatchesHaveDriftedBelowQuantityOnHandIsRejected() {
        Product product = product();
        Inventory inventory = inventoryWithQuantity(product, new BigDecimal("10"));
        StockBatch onlyBatch = batch(new BigDecimal("4"), new BigDecimal("2.00"));

        when(inventoryRepository.findByProductIdForUpdate(product.getId())).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(
                product.getId(), BigDecimal.ZERO)).thenReturn(List.of(onlyBatch));

        assertThatThrownBy(() -> service.stockCut(
                new StockCutRequest(product.getId(), new BigDecimal("10"), QuantityUnit.BASE, StockStrategy.FIFO, null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Insufficient batch stock");
    }

    @Test
    void stockingInAddsANewBatchIncreasesOnHandQuantityAndRecordsAPurchaseExpense() {
        Product product = product();
        Inventory inventory = inventoryWithQuantity(product, new BigDecimal("5"));
        when(inventoryRepository.findByProductIdForUpdate(product.getId())).thenReturn(Optional.of(inventory));
        when(stockMovementMapper.toResponse(any(StockMovement.class), any())).thenReturn(null);

        service.stockIn(new StockInRequest(product.getId(), new BigDecimal("20"), null, new BigDecimal("1.50"), "Delivery"),
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

    @Test
    void aPaidSaleCutsOnlyWhatIsOnHandInsteadOfFailing() {
        Product product = product();
        Inventory inventory = inventoryWithQuantity(product, new BigDecimal("2"));
        StockBatch only = batch(new BigDecimal("2"), new BigDecimal("1.00"));
        when(inventoryRepository.findByProductIdForUpdate(product.getId())).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(
                product.getId(), BigDecimal.ZERO)).thenReturn(List.of(only));
        UUID orderId = UUID.randomUUID();

        service.deductForSale(orderId, List.of(new SaleLine(product.getId(), null, 1, 5)), UUID.randomUUID(), true);

        assertThat(inventory.getQuantityOnHand()).isEqualByComparingTo("0");
        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movement.capture());
        assertThat(movement.getValue().getQuantity()).isEqualByComparingTo("2");
        assertThat(movement.getValue().getReason()).isEqualTo(StockMovementReason.SALE);
        assertThat(movement.getValue().getOrderId()).isEqualTo(orderId);
        assertThat(movement.getValue().getTotalCost()).isEqualByComparingTo("2.00");
    }

    @Test
    void aPaidSaleWithNothingLeftRecordsNothing() {
        Product product = product();
        Inventory inventory = inventoryWithQuantity(product, BigDecimal.ZERO);
        when(inventoryRepository.findByProductIdForUpdate(product.getId())).thenReturn(Optional.of(inventory));

        service.deductForSale(UUID.randomUUID(), List.of(new SaleLine(product.getId(), null, 1, 1)), UUID.randomUUID(),
                true);

        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void anUnpaidSaleThatIsShortIsRefused() {
        Product product = product();
        Inventory inventory = inventoryWithQuantity(product, new BigDecimal("2"));
        when(inventoryRepository.findByProductIdForUpdate(product.getId())).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> service.deductForSale(UUID.randomUUID(),
                List.of(new SaleLine(product.getId(), null, 1, 3)), UUID.randomUUID(), false))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("enough stock");
        assertThat(inventory.getQuantityOnHand()).isEqualByComparingTo("2");
    }

    @Test
    void anOrderIsNeverTakenOutOfStockTwice() {
        UUID orderId = UUID.randomUUID();
        when(stockMovementRepository.existsByOrderIdAndReason(orderId, StockMovementReason.SALE)).thenReturn(true);

        service.deductForSale(orderId, List.of(new SaleLine(UUID.randomUUID(), null, 1, 1)), UUID.randomUUID(), false);

        verify(inventoryRepository, never()).findByProductIdForUpdate(any());
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void requireAvailableForSaleRejectsMoreThanIsOnHand() {
        Product product = product();
        Inventory inventory = inventoryWithQuantity(product, new BigDecimal("23"));
        when(inventoryRepository.findByProductIdIn(any())).thenReturn(List.of(inventory));

        assertThatThrownBy(() -> service.requireAvailableForSale(List.of(new SaleLine(product.getId(), null, 1, 24))))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("enough stock");
        assertThatCode(() -> service.requireAvailableForSale(List.of(new SaleLine(product.getId(), null, 1, 23))))
                .doesNotThrowAnyException();
    }

    @Test
    void aCartonLineNeedsAllTheCansInIt() {
        Product coke = cartonOf24Cans();
        Inventory inventory = inventoryWithQuantity(coke, new BigDecimal("30"));
        when(inventoryRepository.findByProductIdIn(any())).thenReturn(List.of(inventory));

        assertThatThrownBy(() -> service.requireAvailableForSale(List.of(
                new SaleLine(coke.getId(), UUID.randomUUID(), 24, 1),
                new SaleLine(coke.getId(), UUID.randomUUID(), 1, 7))))
                .isInstanceOf(InvalidOperationException.class);
        assertThatCode(() -> service.requireAvailableForSale(List.of(
                new SaleLine(coke.getId(), UUID.randomUUID(), 24, 1),
                new SaleLine(coke.getId(), UUID.randomUUID(), 1, 6))))
                .doesNotThrowAnyException();
    }

    @Test
    void sellingADrinkWithARecipeUsesUpItsIngredientsNotItsOwnStock() {
        Product beans = ingredient("Coffee Beans", SellUnit.GRAM);
        Product milk = ingredient("Fresh Milk", SellUnit.MILLILITER);
        ProductVariant medium = variant(product());
        when(recipeItemRepository.findByVariantIdIn(any())).thenReturn(List.of(
                recipeItem(medium, beans, "18"), recipeItem(medium, milk, "150")));
        Inventory beansStock = inventoryWithQuantity(beans, new BigDecimal("1000"));
        Inventory milkStock = inventoryWithQuantity(milk, new BigDecimal("2000"));
        when(inventoryRepository.findByProductIdForUpdate(beans.getId())).thenReturn(Optional.of(beansStock));
        when(inventoryRepository.findByProductIdForUpdate(milk.getId())).thenReturn(Optional.of(milkStock));
        when(stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(any(), any()))
                .thenAnswer(invocation -> List.of(batch(new BigDecimal("5000"), new BigDecimal("0.02"))));

        service.deductForSale(UUID.randomUUID(),
                List.of(new SaleLine(medium.getProduct().getId(), medium.getId(), 1, 2)), UUID.randomUUID(), false);

        assertThat(beansStock.getQuantityOnHand()).isEqualByComparingTo("964");
        assertThat(milkStock.getQuantityOnHand()).isEqualByComparingTo("1700");
        verify(inventoryRepository, never()).findByProductIdForUpdate(medium.getProduct().getId());
    }

    @Test
    void ingredientsAreLockedInTheSameOrderEveryTimeSoOrdersNeverDeadlock() {
        Product first = ingredient("A", SellUnit.GRAM);
        Product second = ingredient("B", SellUnit.GRAM);
        first.setId(new UUID(0, 1));
        second.setId(new UUID(0, 2));
        ProductVariant drink = variant(product());
        // Listed second-first in the recipe, still locked first-second.
        when(recipeItemRepository.findByVariantIdIn(any())).thenReturn(List.of(
                recipeItem(drink, second, "1"), recipeItem(drink, first, "1")));
        when(inventoryRepository.findByProductIdForUpdate(first.getId()))
                .thenReturn(Optional.of(inventoryWithQuantity(first, BigDecimal.TEN)));
        when(inventoryRepository.findByProductIdForUpdate(second.getId()))
                .thenReturn(Optional.of(inventoryWithQuantity(second, BigDecimal.TEN)));
        when(stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(any(), any()))
                .thenAnswer(invocation -> List.of(batch(BigDecimal.TEN, BigDecimal.ONE)));

        service.deductForSale(UUID.randomUUID(), List.of(new SaleLine(drink.getProduct().getId(), drink.getId(), 1, 1)),
                UUID.randomUUID(), false);

        InOrder lockOrder = inOrder(inventoryRepository);
        lockOrder.verify(inventoryRepository).findByProductIdForUpdate(first.getId());
        lockOrder.verify(inventoryRepository).findByProductIdForUpdate(second.getId());
    }

    @Test
    void anOrderShortOfAnIngredientSaysWhichOne() {
        Product milk = ingredient("Fresh Milk", SellUnit.MILLILITER);
        ProductVariant latte = variant(product());
        when(recipeItemRepository.findByVariantIdIn(any())).thenReturn(List.of(recipeItem(latte, milk, "150")));
        when(inventoryRepository.findByProductIdIn(any())).thenReturn(List.of(inventoryWithQuantity(milk, new BigDecimal("200"))));

        assertThatThrownBy(() -> service.requireAvailableForSale(
                List.of(new SaleLine(latte.getProduct().getId(), latte.getId(), 1, 2))))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Not enough Fresh Milk")
                .hasMessageContaining("200 MILLILITER left, 300 MILLILITER needed");
    }

    @Test
    void sizesShowHowManyCanBeSoldFromStockOrFromTheirIngredients() {
        Product coke = cartonOf24Cans();
        ProductVariant can = variant(coke);
        ProductVariant carton = variant(coke);
        carton.setUnitsPerSale(24);
        Product beans = ingredient("Coffee Beans", SellUnit.GRAM);
        Product milk = ingredient("Fresh Milk", SellUnit.MILLILITER);
        ProductVariant latte = variant(product());
        when(recipeItemRepository.findByVariantIdIn(any())).thenReturn(List.of(
                recipeItem(latte, beans, "18"), recipeItem(latte, milk, "150")));
        when(inventoryRepository.findByProductIdIn(any())).thenReturn(List.of(
                inventoryWithQuantity(coke, new BigDecimal("50")),
                inventoryWithQuantity(beans, new BigDecimal("1000")),
                inventoryWithQuantity(milk, new BigDecimal("700"))));

        Map<UUID, VariantStock> stock = service.variantStock(List.of(can, carton, latte));

        assertThat(stock.get(can.getId())).isEqualTo(new VariantStock(50, false));
        assertThat(stock.get(carton.getId())).isEqualTo(new VariantStock(2, false));
        // Milk runs out first: 700 / 150.
        assertThat(stock.get(latte.getId())).isEqualTo(new VariantStock(4, true));
    }

    @Test
    void wasteIsTakenOutOldestFirstAndCosted() {
        Product milk = ingredient("Fresh Milk", SellUnit.MILLILITER);
        Inventory inventory = inventoryWithQuantity(milk, new BigDecimal("1000"));
        when(inventoryRepository.findByProductIdForUpdate(milk.getId())).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(
                milk.getId(), BigDecimal.ZERO)).thenReturn(List.of(batch(new BigDecimal("1000"), new BigDecimal("0.0025"))));

        StockCutResponse response = service.recordWaste(new WasteRequest(milk.getId(), new BigDecimal("400"), null,
                WasteReason.EXPIRED, "Past date"), UUID.randomUUID());

        assertThat(response.remainingOnHand()).isEqualByComparingTo("600");
        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movement.capture());
        assertThat(movement.getValue().getReason()).isEqualTo(StockMovementReason.WASTE);
        assertThat(movement.getValue().getWasteReason()).isEqualTo(WasteReason.EXPIRED);
        assertThat(movement.getValue().getTotalCost()).isEqualByComparingTo("1.00");
        assertThat(movement.getValue().getBalanceAfter()).isEqualByComparingTo("600");
    }

    @Test
    void returnedCansGoBackAtWhatTheyCostWhenSold() {
        Product coke = cartonOf24Cans();
        Inventory inventory = inventoryWithQuantity(coke, new BigDecimal("10"));
        UUID orderId = UUID.randomUUID();
        when(inventoryRepository.findByProductIdForUpdate(coke.getId())).thenReturn(Optional.of(inventory));
        StockMovement sale = new StockMovement();
        sale.setQuantity(new BigDecimal("3"));
        sale.setTotalCost(new BigDecimal("1.5000"));
        when(stockMovementRepository.findByOrderIdAndProductIdAndReason(orderId, coke.getId(), StockMovementReason.SALE))
                .thenReturn(List.of(sale));

        service.returnToStock(new StockReturnRequest(coke.getId(), new BigDecimal("2"), null, orderId, null),
                UUID.randomUUID());

        assertThat(inventory.getQuantityOnHand()).isEqualByComparingTo("12");
        ArgumentCaptor<StockBatch> batch = ArgumentCaptor.forClass(StockBatch.class);
        verify(stockBatchRepository).save(batch.capture());
        assertThat(batch.getValue().getUnitCost()).isEqualByComparingTo("0.50");
        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movement.capture());
        assertThat(movement.getValue().getReason()).isEqualTo(StockMovementReason.RETURN);
        verify(stockExpenseRepository, never()).save(any());
    }

    @Test
    void moreCannotComeBackThanTheOrderTookOut() {
        Product coke = cartonOf24Cans();
        UUID orderId = UUID.randomUUID();
        when(inventoryRepository.findByProductIdForUpdate(coke.getId()))
                .thenReturn(Optional.of(inventoryWithQuantity(coke, BigDecimal.ZERO)));
        StockMovement sale = new StockMovement();
        sale.setQuantity(new BigDecimal("3"));
        StockMovement earlierReturn = new StockMovement();
        earlierReturn.setQuantity(new BigDecimal("2"));
        when(stockMovementRepository.findByOrderIdAndProductIdAndReason(orderId, coke.getId(), StockMovementReason.SALE))
                .thenReturn(List.of(sale));
        when(stockMovementRepository.findByOrderIdAndProductIdAndReason(orderId, coke.getId(), StockMovementReason.RETURN))
                .thenReturn(List.of(earlierReturn));

        assertThatThrownBy(() -> service.returnToStock(
                new StockReturnRequest(coke.getId(), new BigDecimal("2"), null, orderId, null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Only 1 CAN");
        verify(stockBatchRepository, never()).save(any());
    }

    @Test
    void ingredientsCannotBeReturnedToStock() {
        Product milk = ingredient("Fresh Milk", SellUnit.MILLILITER);
        when(inventoryRepository.findByProductIdForUpdate(milk.getId()))
                .thenReturn(Optional.of(inventoryWithQuantity(milk, BigDecimal.ZERO)));

        assertThatThrownBy(() -> service.returnToStock(
                new StockReturnRequest(milk.getId(), BigDecimal.TEN, null, null, null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("ingredient");
    }

    @Test
    void aCountAboveTheBooksAddsTheDifferenceAtAverageCost() {
        Product coke = cartonOf24Cans();
        Inventory inventory = inventoryWithQuantity(coke, new BigDecimal("20"));
        when(inventoryRepository.findByProductIdForUpdate(coke.getId())).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.valueOnHand(any())).thenReturn(List.of(stockValue(coke.getId(), "20", "10.00")));

        service.adjustToCount(new StockCountRequest(coke.getId(), new BigDecimal("23"), null, null), UUID.randomUUID());

        assertThat(inventory.getQuantityOnHand()).isEqualByComparingTo("23");
        ArgumentCaptor<StockBatch> batch = ArgumentCaptor.forClass(StockBatch.class);
        verify(stockBatchRepository).save(batch.capture());
        assertThat(batch.getValue().getQuantity()).isEqualByComparingTo("3");
        assertThat(batch.getValue().getUnitCost()).isEqualByComparingTo("0.50");
        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movement.capture());
        assertThat(movement.getValue().getReason()).isEqualTo(StockMovementReason.ADJUSTMENT);
        assertThat(movement.getValue().getType()).isEqualTo(StockMovementType.STOCK_IN);
        assertThat(movement.getValue().getNote()).isEqualTo("Stock count");
    }

    @Test
    void aCountBelowTheBooksTakesTheDifferenceOut() {
        Product coke = cartonOf24Cans();
        Inventory inventory = inventoryWithQuantity(coke, new BigDecimal("20"));
        when(inventoryRepository.findByProductIdForUpdate(coke.getId())).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(
                coke.getId(), BigDecimal.ZERO)).thenReturn(List.of(batch(new BigDecimal("20"), new BigDecimal("0.50"))));

        service.adjustToCount(new StockCountRequest(coke.getId(), BigDecimal.ZERO, null, "Shelf empty"),
                UUID.randomUUID());

        assertThat(inventory.getQuantityOnHand()).isEqualByComparingTo("0");
        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movement.capture());
        assertThat(movement.getValue().getType()).isEqualTo(StockMovementType.STOCK_OUT);
        assertThat(movement.getValue().getQuantity()).isEqualByComparingTo("20");
    }

    @Test
    void aCountThatMatchesTheBooksChangesNothing() {
        Product coke = cartonOf24Cans();
        when(inventoryRepository.findByProductIdForUpdate(coke.getId()))
                .thenReturn(Optional.of(inventoryWithQuantity(coke, new BigDecimal("20"))));

        assertThatThrownBy(() -> service.adjustToCount(
                new StockCountRequest(coke.getId(), new BigDecimal("20"), null, null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("already is 20 CAN");
    }

    @Test
    void stockingInCartonsAddsTheCansInThemAndCostsEachCan() {
        Product coke = cartonOf24Cans();
        Inventory inventory = inventoryWithQuantity(coke, new BigDecimal("5"));
        when(inventoryRepository.findByProductIdForUpdate(coke.getId())).thenReturn(Optional.of(inventory));

        service.stockIn(new StockInRequest(coke.getId(), new BigDecimal("2"), QuantityUnit.PURCHASE,
                new BigDecimal("12.50"), "Supplier ABC"), UUID.randomUUID());

        assertThat(inventory.getQuantityOnHand()).isEqualByComparingTo("53");

        ArgumentCaptor<StockBatch> batch = ArgumentCaptor.forClass(StockBatch.class);
        verify(stockBatchRepository).save(batch.capture());
        assertThat(batch.getValue().getQuantity()).isEqualByComparingTo("48");
        assertThat(batch.getValue().getRemainingQuantity()).isEqualByComparingTo("48");
        assertThat(batch.getValue().getUnitCost()).isEqualByComparingTo("0.520833");

        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movement.capture());
        assertThat(movement.getValue().getQuantity()).isEqualByComparingTo("48");

        ArgumentCaptor<StockExpense> expense = ArgumentCaptor.forClass(StockExpense.class);
        verify(stockExpenseRepository).save(expense.capture());
        assertThat(expense.getValue().getQuantity()).isEqualByComparingTo("2");
        assertThat(expense.getValue().getUnit()).isEqualTo("CARTON");
        assertThat(expense.getValue().getUnitCost()).isEqualByComparingTo("12.50");
        assertThat(expense.getValue().getAmount()).isEqualByComparingTo("25.00");
    }

    @Test
    void stockingInLooseCansAddsJustThoseCans() {
        Product coke = cartonOf24Cans();
        Inventory inventory = inventoryWithQuantity(coke, BigDecimal.ZERO);
        when(inventoryRepository.findByProductIdForUpdate(coke.getId())).thenReturn(Optional.of(inventory));

        service.stockIn(new StockInRequest(coke.getId(), new BigDecimal("6"), QuantityUnit.BASE,
                new BigDecimal("0.60"), null), UUID.randomUUID());

        assertThat(inventory.getQuantityOnHand()).isEqualByComparingTo("6");
        ArgumentCaptor<StockBatch> batch = ArgumentCaptor.forClass(StockBatch.class);
        verify(stockBatchRepository).save(batch.capture());
        assertThat(batch.getValue().getUnitCost()).isEqualByComparingTo("0.60");
        ArgumentCaptor<StockExpense> expense = ArgumentCaptor.forClass(StockExpense.class);
        verify(stockExpenseRepository).save(expense.capture());
        assertThat(expense.getValue().getUnit()).isEqualTo("CAN");
        assertThat(expense.getValue().getAmount()).isEqualByComparingTo("3.60");
    }

    @Test
    void stockingInPartOfACartonThatIsNotWholeCansIsRejected() {
        Product coke = cartonOf24Cans();
        Inventory inventory = inventoryWithQuantity(coke, BigDecimal.ZERO);
        when(inventoryRepository.findByProductIdForUpdate(coke.getId())).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> service.stockIn(new StockInRequest(coke.getId(), new BigDecimal("0.3"),
                QuantityUnit.PURCHASE, new BigDecimal("12.50"), null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("whole CAN")
                .hasMessageContaining("7.2");

        verify(stockBatchRepository, never()).save(any());
        assertThat(inventory.getQuantityOnHand()).isEqualByComparingTo("0");
    }

    @Test
    void sellingOneCanTakesExactlyOneCanFromStock() {
        Product coke = cartonOf24Cans();
        Inventory inventory = inventoryWithQuantity(coke, new BigDecimal("24"));
        StockBatch carton = batch(new BigDecimal("24"), new BigDecimal("0.520833"));
        when(inventoryRepository.findByProductIdForUpdate(coke.getId())).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(
                coke.getId(), BigDecimal.ZERO)).thenReturn(List.of(carton));

        StockCutResponse response = service.stockCut(new StockCutRequest(coke.getId(), BigDecimal.ONE,
                QuantityUnit.BASE, StockStrategy.FIFO, "Sold"), UUID.randomUUID());

        assertThat(response.quantityCut()).isEqualByComparingTo("1");
        assertThat(inventory.getQuantityOnHand()).isEqualByComparingTo("23");
        assertThat(carton.getRemainingQuantity()).isEqualByComparingTo("23");
    }

    @Test
    void cuttingAWholeCartonTakesTheCansInIt() {
        Product coke = cartonOf24Cans();
        Inventory inventory = inventoryWithQuantity(coke, new BigDecimal("30"));
        StockBatch batch = batch(new BigDecimal("30"), new BigDecimal("0.50"));
        when(inventoryRepository.findByProductIdForUpdate(coke.getId())).thenReturn(Optional.of(inventory));
        when(stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(
                coke.getId(), BigDecimal.ZERO)).thenReturn(List.of(batch));

        // No unit given: the quantity is in stock units, as the API always took it.
        service.stockCut(new StockCutRequest(coke.getId(), BigDecimal.ONE, null, StockStrategy.FIFO, "Damaged"),
                UUID.randomUUID());

        assertThat(inventory.getQuantityOnHand()).isEqualByComparingTo("6");
    }

    @Test
    void cuttingPartOfACanIsRejected() {
        Product coke = cartonOf24Cans();
        Inventory inventory = inventoryWithQuantity(coke, new BigDecimal("24"));
        when(inventoryRepository.findByProductIdForUpdate(coke.getId())).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> service.stockCut(new StockCutRequest(coke.getId(), new BigDecimal("0.5"),
                QuantityUnit.BASE, StockStrategy.FIFO, null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("whole CAN");
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void notEnoughStockSaysHowManyCansAreLeft() {
        Product coke = cartonOf24Cans();
        Inventory inventory = inventoryWithQuantity(coke, new BigDecimal("10.000"));
        when(inventoryRepository.findByProductIdForUpdate(coke.getId())).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> service.stockCut(new StockCutRequest(coke.getId(), BigDecimal.ONE,
                QuantityUnit.PURCHASE, StockStrategy.FIFO, null), UUID.randomUUID()))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("available 10 CAN, requested 24 CAN");
    }

    private Product ingredient(String name, SellUnit unit) {
        Category ingredients = new Category();
        ingredients.setCategoryGroup(CategoryGroup.INGREDIENT);
        Product product = product();
        product.setName(name);
        product.setCategory(ingredients);
        product.setSellUnit(unit);
        return product;
    }

    private ProductVariant variant(Product product) {
        ProductVariant variant = new ProductVariant();
        variant.setId(UUID.randomUUID());
        variant.setProduct(product);
        return variant;
    }

    private RecipeItem recipeItem(ProductVariant variant, Product ingredient, String quantity) {
        RecipeItem item = new RecipeItem();
        item.setVariant(variant);
        item.setIngredient(ingredient);
        item.setQuantity(new BigDecimal(quantity));
        return item;
    }

    private StockBatchRepository.StockValue stockValue(UUID productId, String quantity, String value) {
        return new StockBatchRepository.StockValue() {
            public UUID getProductId() { return productId; }
            public BigDecimal getQuantity() { return new BigDecimal(quantity); }
            public BigDecimal getValue() { return new BigDecimal(value); }
        };
    }

    private Product cartonOf24Cans() {
        Product product = product();
        product.setName("Coke");
        product.setStockUnit(StockUnit.CARTON);
        product.setSellUnit(SellUnit.CAN);
        product.setUnitsPerStock(new BigDecimal("24"));
        return product;
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
