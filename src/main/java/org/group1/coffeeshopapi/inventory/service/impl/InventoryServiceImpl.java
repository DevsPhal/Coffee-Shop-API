package org.group1.coffeeshopapi.inventory.service.impl;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.group1.coffeeshopapi.common.enums.QuantityUnit;
import org.group1.coffeeshopapi.common.enums.StockMovementReason;
import org.group1.coffeeshopapi.common.enums.StockMovementType;
import org.group1.coffeeshopapi.common.enums.StockStrategy;
import org.group1.coffeeshopapi.common.enums.WasteReason;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.inventory.dto.request.MovementSearch;
import org.group1.coffeeshopapi.inventory.dto.request.SaleLine;
import org.group1.coffeeshopapi.inventory.dto.request.StockCountRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockCutRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockInRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockReturnRequest;
import org.group1.coffeeshopapi.inventory.dto.request.WasteRequest;
import org.group1.coffeeshopapi.inventory.dto.response.BatchConsumptionResponse;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.inventory.dto.response.StockCutResponse;
import org.group1.coffeeshopapi.inventory.dto.response.StockMovementResponse;
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
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.inventory.service.VariantStock;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.recipe.entity.RecipeItem;
import org.group1.coffeeshopapi.recipe.repository.RecipeItemRepository;
import org.group1.coffeeshopapi.user.dto.response.ActorSummary;
import org.group1.coffeeshopapi.user.service.ActorLookupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private static final int BASE_UNIT_COST_SCALE = 6;
    private static final int TOTAL_COST_SCALE = 4;

    private final InventoryRepository inventoryRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockExpenseRepository stockExpenseRepository;
    private final RecipeItemRepository recipeItemRepository;
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
        QuantityUnit unit = unitOr(request.unit(), QuantityUnit.PURCHASE);
        BigDecimal baseQuantity = toWholeBaseUnits(product, request.quantity(), unit);
        BigDecimal amount = request.quantity().multiply(request.unitCost());

        addToStock(inventory, baseQuantity,
                request.unitCost().divide(product.baseUnitsPer(unit), BASE_UNIT_COST_SCALE, RoundingMode.HALF_UP));
        StockMovement movement = record(inventory, StockMovementType.STOCK_IN, StockMovementReason.PURCHASE,
                baseQuantity, amount, null, request.note(), performedBy);
        recordStockPurchaseExpense(product, movement, request.quantity(), product.unitName(unit), request.unitCost());

        return toResponse(movement);
    }

    // The expense keeps the purchase as it was bought (2 CARTON at $12.50), so it matches the supplier's invoice.
    private void recordStockPurchaseExpense(Product product, StockMovement movement, BigDecimal quantity, String unit,
            BigDecimal unitCost) {
        StockExpense expense = new StockExpense();
        expense.setProduct(product);
        expense.setStockMovement(movement);
        expense.setQuantity(quantity);
        expense.setUnit(unit);
        expense.setUnitCost(unitCost);
        expense.setAmount(quantity.multiply(unitCost));
        expense.setExpenseDate(LocalDate.now());
        stockExpenseRepository.save(expense);
    }

    @Override
    @Transactional
    public StockCutResponse stockCut(StockCutRequest request, UUID performedBy) {
        Inventory inventory = findInventoryForUpdate(request.productId());
        BigDecimal quantity = toWholeBaseUnits(inventory.getProduct(), request.quantity(),
                unitOr(request.unit(), QuantityUnit.PURCHASE));
        requireOnHand(inventory, quantity);
        return toCutResponse(cut(inventory, request.strategy(), quantity, StockMovementReason.ADJUSTMENT, null, null,
                request.note(), performedBy, false));
    }

    @Override
    @Transactional
    public StockCutResponse recordWaste(WasteRequest request, UUID performedBy) {
        Inventory inventory = findInventoryForUpdate(request.productId());
        BigDecimal quantity = toWholeBaseUnits(inventory.getProduct(), request.quantity(),
                unitOr(request.unit(), QuantityUnit.BASE));
        requireOnHand(inventory, quantity);
        return toCutResponse(cut(inventory, StockStrategy.FIFO, quantity, StockMovementReason.WASTE,
                request.wasteReason(), null, request.note(), performedBy, false));
    }

    @Override
    @Transactional
    public StockMovementResponse returnToStock(StockReturnRequest request, UUID performedBy) {
        Inventory inventory = findInventoryForUpdate(request.productId());
        Product product = inventory.getProduct();
        if (product.isIngredient()) {
            throw new InvalidOperationException("'" + product.getName() + "' is an ingredient — it was used up in "
                    + "drinks and can't come back to stock; record waste or a stock count instead");
        }
        BigDecimal quantity = toWholeBaseUnits(product, request.quantity(), unitOr(request.unit(), QuantityUnit.BASE));

        BigDecimal unitCost;
        if (request.orderId() != null) {
            unitCost = returnableUnitCost(request.orderId(), product, quantity);
        } else {
            unitCost = averageUnitCosts(List.of(product.getId())).get(product.getId());
        }

        addToStock(inventory, quantity, unitCost);
        StockMovement movement = record(inventory, StockMovementType.STOCK_IN, StockMovementReason.RETURN, quantity,
                quantity.multiply(unitCost), request.orderId(), request.note(), performedBy);
        return toResponse(movement);
    }

    // No more can come back than the order took out, and it goes back at what it cost when sold.
    private BigDecimal returnableUnitCost(UUID orderId, Product product, BigDecimal quantity) {
        List<StockMovement> sold = stockMovementRepository.findByOrderIdAndProductIdAndReason(
                orderId, product.getId(), StockMovementReason.SALE);
        BigDecimal soldQuantity = sum(sold, StockMovement::getQuantity);
        if (soldQuantity.signum() == 0) {
            throw new InvalidOperationException("That order didn't take any '" + product.getName() + "' out of stock");
        }
        BigDecimal returned = sum(stockMovementRepository.findByOrderIdAndProductIdAndReason(
                orderId, product.getId(), StockMovementReason.RETURN), StockMovement::getQuantity);
        BigDecimal returnable = soldQuantity.subtract(returned);
        if (quantity.compareTo(returnable) > 0) {
            throw new InvalidOperationException("Only " + describe(returnable, product) + " of '" + product.getName()
                    + "' from that order can still come back");
        }
        BigDecimal soldCost = sum(sold, StockMovement::getTotalCost);
        return soldCost.divide(soldQuantity, BASE_UNIT_COST_SCALE, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public StockMovementResponse adjustToCount(StockCountRequest request, UUID performedBy) {
        Inventory inventory = findInventoryForUpdate(request.productId());
        Product product = inventory.getProduct();
        BigDecimal counted = request.countedQuantity().signum() == 0
                ? BigDecimal.ZERO
                : toWholeBaseUnits(product, request.countedQuantity(), unitOr(request.unit(), QuantityUnit.BASE));
        BigDecimal difference = counted.subtract(inventory.getQuantityOnHand());
        if (difference.signum() == 0) {
            throw new InvalidOperationException("Stock of '" + product.getName() + "' already is "
                    + describe(counted, product));
        }
        String note = request.note() != null && !request.note().isBlank() ? request.note() : "Stock count";
        if (difference.signum() < 0) {
            return toResponse(cut(inventory, StockStrategy.FIFO, difference.negate(), StockMovementReason.ADJUSTMENT,
                    null, null, note, performedBy, false).movement());
        }
        BigDecimal unitCost = averageUnitCosts(List.of(product.getId())).get(product.getId());
        addToStock(inventory, difference, unitCost);
        return toResponse(record(inventory, StockMovementType.STOCK_IN, StockMovementReason.ADJUSTMENT, difference,
                difference.multiply(unitCost), null, note, performedBy));
    }

    @Override
    public void requireAvailableForSale(List<SaleLine> lines) {
        Map<UUID, BigDecimal> needed = requirements(lines);
        Map<UUID, Inventory> inventories = inventoryRepository.findByProductIdIn(needed.keySet()).stream()
                .collect(Collectors.toMap(inventory -> inventory.getProduct().getId(), Function.identity()));
        needed.forEach((productId, quantity) -> {
            Inventory inventory = inventories.get(productId);
            if (inventory == null) {
                throw new ResourceNotFoundException("Inventory not found for product");
            }
            if (inventory.getQuantityOnHand().compareTo(quantity) < 0) {
                throw notEnoughForSale(inventory, quantity);
            }
        });
    }

    @Override
    @Transactional
    public void deductForSale(UUID orderId, List<SaleLine> lines, UUID performedBy, boolean capToAvailable) {
        if (orderId != null && stockMovementRepository.existsByOrderIdAndReason(orderId, StockMovementReason.SALE)) {
            log.warn("Order {} was already taken out of stock — not deducting it twice", orderId);
            return;
        }
        // Sorted by product id, so two orders sharing milk and beans lock them in the same order and never deadlock.
        for (Map.Entry<UUID, BigDecimal> need : requirements(lines).entrySet()) {
            Inventory inventory = findInventoryForUpdate(need.getKey());
            BigDecimal quantity = need.getValue();
            if (inventory.getQuantityOnHand().compareTo(quantity) < 0) {
                if (!capToAvailable) {
                    throw notEnoughForSale(inventory, quantity);
                }
                log.warn("Stock short for product {}: sold {}, only {} on hand — cutting what's there",
                        need.getKey(), quantity, inventory.getQuantityOnHand());
                quantity = inventory.getQuantityOnHand();
            }
            if (quantity.signum() > 0) {
                cut(inventory, StockStrategy.FIFO, quantity, StockMovementReason.SALE, null, orderId,
                        "Sold in order " + orderId, performedBy, capToAvailable);
            }
        }
    }

    @Override
    @Transactional
    public void reverseSaleForCancelledOrder(UUID orderId, boolean backToStock, UUID performedBy, String note) {
        // Grouped by product id, sorted, so concurrent cancellations lock stock rows in the same order.
        Map<UUID, List<StockMovement>> soldByProduct = new TreeMap<>(stockMovementRepository
                .findByOrderIdAndReason(orderId, StockMovementReason.SALE).stream()
                .collect(Collectors.groupingBy(movement -> movement.getProduct().getId())));
        for (Map.Entry<UUID, List<StockMovement>> sold : soldByProduct.entrySet()) {
            Inventory inventory = findInventoryForUpdate(sold.getKey());
            BigDecimal soldQuantity = sum(sold.getValue(), StockMovement::getQuantity);
            BigDecimal returned = sum(stockMovementRepository.findByOrderIdAndProductIdAndReason(
                    orderId, sold.getKey(), StockMovementReason.RETURN), StockMovement::getQuantity);
            BigDecimal quantity = soldQuantity.subtract(returned);
            if (quantity.signum() <= 0) {
                continue;
            }
            BigDecimal unitCost = sum(sold.getValue(), StockMovement::getTotalCost)
                    .divide(soldQuantity, BASE_UNIT_COST_SCALE, RoundingMode.HALF_UP);
            addToStock(inventory, quantity, unitCost);
            record(inventory, StockMovementType.STOCK_IN, StockMovementReason.RETURN, quantity,
                    quantity.multiply(unitCost), orderId, note, performedBy);
            if (!backToStock) {
                cut(inventory, StockStrategy.FIFO, quantity, StockMovementReason.WASTE, WasteReason.OTHER, orderId,
                        note, performedBy, false);
            }
        }
    }

    private InvalidOperationException notEnoughForSale(Inventory inventory, BigDecimal needed) {
        Product product = inventory.getProduct();
        if (product.isIngredient()) {
            return new InvalidOperationException("Not enough " + product.getName() + " left to make this order ("
                    + describe(inventory.getQuantityOnHand(), product) + " left, " + describe(needed, product)
                    + " needed)");
        }
        return new InvalidOperationException("'" + product.getName() + "' doesn't have enough stock left for this order");
    }

    // What an order takes out of stock, per stock item: a recipe's ingredients, or the product's own base units.
    private Map<UUID, BigDecimal> requirements(List<SaleLine> lines) {
        Map<UUID, List<RecipeItem>> recipes = recipesByVariant(lines.stream()
                .map(SaleLine::variantId).filter(id -> id != null).collect(Collectors.toSet()));
        Map<UUID, BigDecimal> needed = new TreeMap<>();
        for (SaleLine line : lines) {
            BigDecimal sold = BigDecimal.valueOf(line.quantity());
            List<RecipeItem> recipe = line.variantId() != null ? recipes.get(line.variantId()) : null;
            if (recipe != null && !recipe.isEmpty()) {
                for (RecipeItem item : recipe) {
                    needed.merge(item.getIngredient().getId(), item.getQuantity().multiply(sold), BigDecimal::add);
                }
            } else {
                needed.merge(line.productId(), sold.multiply(BigDecimal.valueOf(Math.max(1, line.unitsPerSale()))),
                        BigDecimal::add);
            }
        }
        return needed;
    }

    private Map<UUID, List<RecipeItem>> recipesByVariant(Collection<UUID> variantIds) {
        if (variantIds.isEmpty()) {
            return Map.of();
        }
        return recipeItemRepository.findByVariantIdIn(variantIds).stream()
                .collect(Collectors.groupingBy(item -> item.getVariant().getId()));
    }

    @Override
    public Map<UUID, VariantStock> variantStock(Collection<ProductVariant> variants) {
        if (variants.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<RecipeItem>> recipes = recipesByVariant(
                variants.stream().map(ProductVariant::getId).collect(Collectors.toSet()));
        Set<UUID> stockIds = new HashSet<>();
        for (ProductVariant variant : variants) {
            List<RecipeItem> recipe = recipes.get(variant.getId());
            if (recipe == null) {
                stockIds.add(variant.getProduct().getId());
            } else {
                recipe.forEach(item -> stockIds.add(item.getIngredient().getId()));
            }
        }
        Map<UUID, BigDecimal> onHand = inventoryRepository.findByProductIdIn(stockIds).stream()
                .collect(Collectors.toMap(inventory -> inventory.getProduct().getId(), Inventory::getQuantityOnHand));

        Map<UUID, VariantStock> result = new HashMap<>();
        for (ProductVariant variant : variants) {
            List<RecipeItem> recipe = recipes.get(variant.getId());
            if (recipe == null) {
                int perSale = variant.getUnitsPerSale() != null ? Math.max(1, variant.getUnitsPerSale()) : 1;
                result.put(variant.getId(), new VariantStock(
                        servings(onHand.get(variant.getProduct().getId()), BigDecimal.valueOf(perSale)), false));
            } else {
                int available = Integer.MAX_VALUE;
                for (RecipeItem item : recipe) {
                    available = Math.min(available, servings(onHand.get(item.getIngredient().getId()), item.getQuantity()));
                }
                result.put(variant.getId(), new VariantStock(available, true));
            }
        }
        return result;
    }

    private static int servings(BigDecimal onHand, BigDecimal perServing) {
        if (onHand == null || onHand.signum() <= 0 || perServing.signum() <= 0) {
            return 0;
        }
        return onHand.divide(perServing, 0, RoundingMode.DOWN).min(BigDecimal.valueOf(Integer.MAX_VALUE)).intValue();
    }

    @Override
    public Map<UUID, BigDecimal> averageUnitCosts(Collection<UUID> productIds) {
        Map<UUID, BigDecimal> costs = new HashMap<>();
        if (productIds.isEmpty()) {
            return costs;
        }
        for (StockBatchRepository.StockValue value : stockBatchRepository.valueOnHand(productIds)) {
            if (value.getQuantity() != null && value.getQuantity().signum() > 0) {
                costs.put(value.getProductId(),
                        value.getValue().divide(value.getQuantity(), BASE_UNIT_COST_SCALE, RoundingMode.HALF_UP));
            }
        }
        for (UUID productId : productIds) {
            costs.computeIfAbsent(productId, id -> stockBatchRepository.findFirstByProductIdOrderByCreatedAtDesc(id)
                    .map(StockBatch::getUnitCost)
                    .orElse(BigDecimal.ZERO));
        }
        return costs;
    }

    @Override
    public Page<StockMovementResponse> listMovements(UUID productId, Pageable pageable) {
        return withActors(stockMovementRepository.findByProductId(productId, pageable));
    }

    @Override
    public Page<StockMovementResponse> searchMovements(MovementSearch search, Pageable pageable) {
        Specification<StockMovement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search.productId() != null) {
                predicates.add(cb.equal(root.get("product").get("id"), search.productId()));
            }
            if (search.type() != null) {
                predicates.add(cb.equal(root.get("type"), search.type()));
            }
            if (search.reason() != null) {
                predicates.add(cb.equal(root.get("reason"), search.reason()));
            }
            if (search.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), search.from().atStartOfDay()));
            }
            if (search.to() != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), search.to().plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return withActors(stockMovementRepository.findAll(spec, pageable));
    }

    private Page<StockMovementResponse> withActors(Page<StockMovement> movements) {
        Set<UUID> actorIds = new HashSet<>();
        for (StockMovement movement : movements) {
            actorIds.add(movement.getPerformedBy());
        }
        Map<UUID, ActorSummary> actors = actorLookupService.resolveAll(actorIds);
        return movements.map(movement -> stockMovementMapper.toResponse(movement, actors.get(movement.getPerformedBy())));
    }

    private record CutResult(StockMovement movement, BigDecimal quantity, List<BatchConsumptionResponse> consumptions) {
    }

    // Takes stock from batches oldest-first (or newest-first) and records what it was worth.
    // allowShort cuts only what the batches still hold rather than failing, for sales that are already paid.
    private CutResult cut(Inventory inventory, StockStrategy strategy, BigDecimal requestedQuantity,
            StockMovementReason reason, WasteReason wasteReason, UUID orderId, String note, UUID performedBy,
            boolean allowShort) {
        Product product = inventory.getProduct();

        List<StockBatch> batches = strategy == StockStrategy.LIFO
                ? stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtDesc(
                        product.getId(), BigDecimal.ZERO)
                : stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(
                        product.getId(), BigDecimal.ZERO);

        List<BatchConsumptionResponse> consumptions = new ArrayList<>();
        List<StockBatch> touchedBatches = new ArrayList<>();
        BigDecimal remainingToCut = requestedQuantity;
        BigDecimal cost = BigDecimal.ZERO;

        for (StockBatch batch : batches) {
            if (remainingToCut.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal takeFromBatch = batch.getRemainingQuantity().min(remainingToCut);
            batch.setRemainingQuantity(batch.getRemainingQuantity().subtract(takeFromBatch));
            remainingToCut = remainingToCut.subtract(takeFromBatch);
            cost = cost.add(takeFromBatch.multiply(batch.getUnitCost()));

            touchedBatches.add(batch);
            consumptions.add(new BatchConsumptionResponse(
                    batch.getId(), batch.getCreatedAt(), takeFromBatch, batch.getUnitCost()));
        }

        BigDecimal quantity = requestedQuantity;
        if (remainingToCut.compareTo(BigDecimal.ZERO) > 0) {
            if (!allowShort) {
                throw new InvalidOperationException("Insufficient batch stock to fulfill this cut");
            }
            log.warn("Batches of product {} hold {} less than its quantity on hand", product.getId(), remainingToCut);
            quantity = requestedQuantity.subtract(remainingToCut);
        }

        stockBatchRepository.saveAll(touchedBatches);

        inventory.setQuantityOnHand(inventory.getQuantityOnHand().subtract(quantity));
        inventoryRepository.save(inventory);

        StockMovement movement = movement(inventory, StockMovementType.STOCK_OUT, reason, quantity, cost, orderId, note,
                performedBy);
        movement.setStrategy(strategy);
        movement.setWasteReason(wasteReason);
        stockMovementRepository.save(movement);
        return new CutResult(movement, quantity, consumptions);
    }

    private void addToStock(Inventory inventory, BigDecimal baseQuantity, BigDecimal baseUnitCost) {
        StockBatch batch = new StockBatch();
        batch.setProduct(inventory.getProduct());
        batch.setQuantity(baseQuantity);
        batch.setRemainingQuantity(baseQuantity);
        batch.setUnitCost(baseUnitCost);
        stockBatchRepository.save(batch);

        inventory.setQuantityOnHand(inventory.getQuantityOnHand().add(baseQuantity));
        inventoryRepository.save(inventory);
    }

    private StockMovement record(Inventory inventory, StockMovementType type, StockMovementReason reason,
            BigDecimal quantity, BigDecimal totalCost, UUID orderId, String note, UUID performedBy) {
        StockMovement movement = movement(inventory, type, reason, quantity, totalCost, orderId, note, performedBy);
        stockMovementRepository.save(movement);
        return movement;
    }

    private static StockMovement movement(Inventory inventory, StockMovementType type, StockMovementReason reason,
            BigDecimal quantity, BigDecimal totalCost, UUID orderId, String note, UUID performedBy) {
        StockMovement movement = new StockMovement();
        movement.setProduct(inventory.getProduct());
        movement.setType(type);
        movement.setReason(reason);
        movement.setQuantity(quantity);
        movement.setTotalCost(totalCost.setScale(TOTAL_COST_SCALE, RoundingMode.HALF_UP));
        movement.setBalanceAfter(inventory.getQuantityOnHand());
        movement.setOrderId(orderId);
        movement.setNote(note);
        movement.setPerformedBy(performedBy);
        return movement;
    }

    private StockCutResponse toCutResponse(CutResult result) {
        StockMovement movement = result.movement();
        return new StockCutResponse(movement.getId(), movement.getProduct().getId(), movement.getStrategy(),
                result.quantity(), movement.getBalanceAfter(), result.consumptions());
    }

    private static void requireOnHand(Inventory inventory, BigDecimal quantity) {
        if (inventory.getQuantityOnHand().compareTo(quantity) < 0) {
            Product product = inventory.getProduct();
            throw new InvalidOperationException("Insufficient stock: available "
                    + describe(inventory.getQuantityOnHand(), product) + ", requested " + describe(quantity, product));
        }
    }

    private static QuantityUnit unitOr(QuantityUnit unit, QuantityUnit fallback) {
        return unit != null ? unit : fallback;
    }

    // Everything is counted whole (a can, a cup, a gram), so stock never holds part of one.
    private static BigDecimal toWholeBaseUnits(Product product, BigDecimal quantity, QuantityUnit unit) {
        BigDecimal base = product.toBaseUnits(quantity, unit);
        if (base.signum() <= 0 || base.stripTrailingZeros().scale() > 0) {
            String asked = unit == QuantityUnit.BASE ? "" : " (" + plain(quantity) + " " + product.unitName(unit) + ")";
            throw new InvalidOperationException("Stock of '" + product.getName() + "' is counted in whole "
                    + product.unitName(QuantityUnit.BASE) + " — " + plain(base) + asked + " is not a whole number");
        }
        return base.setScale(0, RoundingMode.UNNECESSARY);
    }

    private static BigDecimal sum(List<StockMovement> movements, Function<StockMovement, BigDecimal> value) {
        return movements.stream().map(value).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String describe(BigDecimal baseQuantity, Product product) {
        return plain(baseQuantity) + " " + product.unitName(QuantityUnit.BASE);
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
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
