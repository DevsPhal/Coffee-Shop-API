package org.group1.coffeeshopapi.inventory;

import org.group1.coffeeshopapi.category.entity.Category;
import org.group1.coffeeshopapi.category.repository.CategoryRepository;
import org.group1.coffeeshopapi.common.enums.CategoryGroup;
import org.group1.coffeeshopapi.common.enums.QuantityUnit;
import org.group1.coffeeshopapi.common.enums.ReportPeriod;
import org.group1.coffeeshopapi.common.enums.SellUnit;
import org.group1.coffeeshopapi.common.enums.StockMovementReason;
import org.group1.coffeeshopapi.common.enums.StockUnit;
import org.group1.coffeeshopapi.common.enums.VariantLabel;
import org.group1.coffeeshopapi.common.enums.WasteReason;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.inventory.dto.request.SaleLine;
import org.group1.coffeeshopapi.inventory.dto.request.StockCountRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockInRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockReturnRequest;
import org.group1.coffeeshopapi.inventory.dto.request.WasteRequest;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryReportResponse;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryReportRow;
import org.group1.coffeeshopapi.inventory.entity.Inventory;
import org.group1.coffeeshopapi.inventory.entity.StockBatch;
import org.group1.coffeeshopapi.inventory.entity.StockMovement;
import org.group1.coffeeshopapi.inventory.repository.InventoryRepository;
import org.group1.coffeeshopapi.inventory.repository.StockBatchRepository;
import org.group1.coffeeshopapi.inventory.repository.StockExpenseRepository;
import org.group1.coffeeshopapi.inventory.repository.StockMovementRepository;
import org.group1.coffeeshopapi.inventory.service.InventoryReportService;
import org.group1.coffeeshopapi.inventory.service.InventoryService;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.group1.coffeeshopapi.product.repository.ProductRepository;
import org.group1.coffeeshopapi.product.repository.ProductVariantRepository;
import org.group1.coffeeshopapi.recipe.dto.request.RecipeItemRequest;
import org.group1.coffeeshopapi.recipe.dto.request.SetRecipeRequest;
import org.group1.coffeeshopapi.recipe.dto.response.RecipeResponse;
import org.group1.coffeeshopapi.recipe.repository.RecipeItemRepository;
import org.group1.coffeeshopapi.recipe.service.RecipeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Runs against the real schema and transactions (no mocks), committing each step like the app does.
@SpringBootTest
@ActiveProfiles("test")
class InventoryWorkflowIntegrationTest {

    @Autowired private InventoryService inventoryService;
    @Autowired private InventoryReportService inventoryReportService;
    @Autowired private RecipeService recipeService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private StockBatchRepository stockBatchRepository;
    @Autowired private StockMovementRepository stockMovementRepository;
    @Autowired private StockExpenseRepository stockExpenseRepository;
    @Autowired private RecipeItemRepository recipeItemRepository;

    private final UUID admin = UUID.randomUUID();
    private final List<Category> categories = new ArrayList<>();
    private final List<Product> products = new ArrayList<>();

    @AfterEach
    void removeWhatTheTestCreated() {
        List<UUID> productIds = products.stream().map(Product::getId).toList();
        for (UUID productId : productIds) {
            recipeItemRepository.deleteAll(recipeItemRepository.findByProductId(productId));
        }
        stockExpenseRepository.deleteAll(stockExpenseRepository.findAll().stream()
                .filter(expense -> productIds.contains(expense.getProduct().getId())).toList());
        stockMovementRepository.deleteAll(stockMovementRepository.findAll().stream()
                .filter(movement -> productIds.contains(movement.getProduct().getId())).toList());
        stockBatchRepository.deleteAll(stockBatchRepository.findAll().stream()
                .filter(batch -> productIds.contains(batch.getProduct().getId())).toList());
        inventoryRepository.deleteAll(inventoryRepository.findByProductIdIn(productIds));
        variantRepository.deleteAll(variantRepository.findByProductIdInOrderBySortOrderAscNameAsc(productIds));
        productRepository.deleteAll(products);
        categoryRepository.deleteAll(categories);
    }

    @Test
    void cartonsCansRecipesWasteReturnsAndCountsStayConsistent() {
        Category drinks = category("Cold Drinks " + UUID.randomUUID(), CategoryGroup.BEVERAGE);
        Category ingredients = category("Coffee Ingredients " + UUID.randomUUID(), CategoryGroup.INGREDIENT);
        Category coffee = category("Coffee " + UUID.randomUUID(), CategoryGroup.FRESH_DRINK);
        Product coke = product("Coke", drinks, StockUnit.CARTON, SellUnit.CAN, 24);
        ProductVariant can = variant(coke, VariantLabel.MEDIUM, "0.75", 1);
        ProductVariant carton = variant(coke, VariantLabel.CARTON, "16.00", 24);

        // Buying 2 cartons adds 48 cans; the expense keeps the invoice as 2 cartons.
        inventoryService.stockIn(new StockInRequest(coke.getId(), new BigDecimal("2"), QuantityUnit.PURCHASE,
                new BigDecimal("12.00"), "Supplier ABC"), admin);
        assertThat(onHand(coke)).isEqualByComparingTo("48");

        // 3 single cans and a full carton: 27 cans, nothing fractional.
        UUID orderA = UUID.randomUUID();
        inventoryService.deductForSale(orderA, List.of(line(can, 3), line(carton, 1)), admin, false);
        assertThat(onHand(coke)).isEqualByComparingTo("21");

        // Paying again (a retried request) doesn't take the order out twice.
        inventoryService.deductForSale(orderA, List.of(line(can, 3), line(carton, 1)), admin, false);
        assertThat(onHand(coke)).isEqualByComparingTo("21");

        // Not enough cans for another carton.
        assertThatThrownBy(() -> inventoryService.requireAvailableForSale(List.of(line(carton, 1))))
                .isInstanceOf(InvalidOperationException.class);

        inventoryService.recordWaste(new WasteRequest(coke.getId(), BigDecimal.ONE, QuantityUnit.BASE,
                WasteReason.DAMAGED, "Dented"), admin);
        assertThat(onHand(coke)).isEqualByComparingTo("20");

        inventoryService.returnToStock(new StockReturnRequest(coke.getId(), new BigDecimal("2"), null, orderA, null),
                admin);
        assertThat(onHand(coke)).isEqualByComparingTo("22");
        assertThatThrownBy(() -> inventoryService.returnToStock(
                new StockReturnRequest(coke.getId(), new BigDecimal("26"), null, orderA, null), admin))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Only 25 CAN");

        inventoryService.adjustToCount(new StockCountRequest(coke.getId(), new BigDecimal("20"), null, null), admin);
        assertThat(onHand(coke)).isEqualByComparingTo("20");

        // Coffee is made from ingredients bought by the kilogram and litre and counted in grams and millilitres.
        Product beans = product("Coffee Beans", ingredients, StockUnit.KILOGRAM, SellUnit.GRAM, 1000);
        Product milk = product("Fresh Milk", ingredients, StockUnit.LITER, SellUnit.MILLILITER, 1000);
        Product latte = product("Iced Latte", coffee, StockUnit.PACK, SellUnit.CUP, 1);
        ProductVariant medium = variant(latte, VariantLabel.MEDIUM, "2.50", 1);
        inventoryService.stockIn(new StockInRequest(beans.getId(), BigDecimal.ONE, QuantityUnit.PURCHASE,
                new BigDecimal("15.00"), null), admin);
        inventoryService.stockIn(new StockInRequest(milk.getId(), new BigDecimal("2"), QuantityUnit.PURCHASE,
                new BigDecimal("1.50"), null), admin);

        RecipeResponse recipe = recipeService.setRecipe(latte.getId(), medium.getId(), new SetRecipeRequest(List.of(
                new RecipeItemRequest(beans.getId(), new BigDecimal("18")),
                new RecipeItemRequest(milk.getId(), new BigDecimal("150")))));
        assertThat(recipe.cost()).isEqualByComparingTo("0.495");
        assertThat(recipe.margin()).isEqualByComparingTo("2.005");
        // Milk runs out first: 2000 / 150.
        assertThat(recipe.availableQuantity()).isEqualTo(13);

        inventoryService.deductForSale(UUID.randomUUID(), List.of(line(medium, 2)), admin, false);
        assertThat(onHand(beans)).isEqualByComparingTo("964");
        assertThat(onHand(milk)).isEqualByComparingTo("1700");
        assertThat(onHand(latte)).isEqualByComparingTo("0");

        assertThatThrownBy(() -> inventoryService.requireAvailableForSale(List.of(line(medium, 12))))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Not enough Fresh Milk");

        // The latte shows on the menu because its ingredients are there, even with no stock of its own.
        assertThat(productRepository.findByCategoryIdAndStatusAndInStockOrderByNameAsc(coffee.getId(),
                org.group1.coffeeshopapi.common.enums.Status.ACTIVE)).extracting(Product::getId).contains(latte.getId());
        assertThat(productRepository.findByCategoryIdAndStatusAndInStockOrderByNameAsc(ingredients.getId(),
                org.group1.coffeeshopapi.common.enums.Status.ACTIVE)).isEmpty();

        for (Product product : List.of(coke, beans, milk)) {
            assertThat(batchTotal(product)).as(product.getName() + " batches match on hand")
                    .isEqualByComparingTo(onHand(product));
        }

        InventoryReportResponse report = inventoryReportService.summary(ReportPeriod.DAY, LocalDate.now());
        InventoryReportRow cokeRow = report.products().stream()
                .filter(row -> row.productId().equals(coke.getId())).findFirst().orElseThrow();
        assertThat(cokeRow.purchased()).isEqualByComparingTo("48");
        assertThat(cokeRow.sold()).isEqualByComparingTo("27");
        assertThat(cokeRow.wasted()).isEqualByComparingTo("1");
        assertThat(cokeRow.returned()).isEqualByComparingTo("2");
        assertThat(cokeRow.adjustedOut()).isEqualByComparingTo("2");
        assertThat(cokeRow.purchaseCost()).isEqualByComparingTo("24.00");
        assertThat(cokeRow.salesCost()).isEqualByComparingTo("13.50");
        assertThat(cokeRow.wasteCost()).isEqualByComparingTo("0.50");
        assertThat(cokeRow.stockValue()).isEqualByComparingTo("10.00");
        InventoryReportRow milkRow = report.products().stream()
                .filter(row -> row.productId().equals(milk.getId())).findFirst().orElseThrow();
        assertThat(milkRow.ingredient()).isTrue();
        assertThat(milkRow.sold()).isEqualByComparingTo("300");
        assertThat(milkRow.salesCost()).isEqualByComparingTo("0.45");
        assertThat(report.wasteCostByReason()).containsKey(WasteReason.DAMAGED);

        List<StockMovement> cokeHistory = stockMovementRepository.findAll().stream()
                .filter(movement -> movement.getProduct().getId().equals(coke.getId())).toList();
        assertThat(cokeHistory).extracting(StockMovement::getReason).containsOnly(StockMovementReason.PURCHASE,
                StockMovementReason.SALE, StockMovementReason.WASTE, StockMovementReason.RETURN,
                StockMovementReason.ADJUSTMENT);
        assertThat(cokeHistory).allSatisfy(movement -> assertThat(movement.getBalanceAfter()).isNotNull());
    }

    @Test
    void salesAtTheSameMomentNeverSellMoreThanIsThere() throws Exception {
        Category drinks = category("Fizzy " + UUID.randomUUID(), CategoryGroup.BEVERAGE);
        Product sprite = product("Sprite", drinks, StockUnit.CARTON, SellUnit.CAN, 24);
        ProductVariant can = variant(sprite, VariantLabel.MEDIUM, "0.75", 1);
        inventoryService.stockIn(new StockInRequest(sprite.getId(), new BigDecimal("30"), QuantityUnit.BASE,
                new BigDecimal("0.50"), null), admin);

        int buyers = 20;
        ExecutorService pool = Executors.newFixedThreadPool(buyers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger sold = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();
        List<Future<?>> sales = new ArrayList<>();
        for (int i = 0; i < buyers; i++) {
            sales.add(pool.submit(() -> {
                start.await();
                try {
                    inventoryService.deductForSale(UUID.randomUUID(), List.of(line(can, 2)), admin, false);
                    sold.incrementAndGet();
                } catch (InvalidOperationException e) {
                    refused.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> sale : sales) {
            sale.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        // 30 cans, 2 per sale: exactly 15 go through and the rest are refused, never a negative count.
        assertThat(sold.get()).isEqualTo(15);
        assertThat(refused.get()).isEqualTo(5);
        assertThat(onHand(sprite)).isEqualByComparingTo("0");
        assertThat(batchTotal(sprite)).isEqualByComparingTo("0");
        assertThat(stockMovementRepository.findAll().stream()
                .filter(movement -> movement.getProduct().getId().equals(sprite.getId())
                        && movement.getReason() == StockMovementReason.SALE)
                .count()).isEqualTo(15);
    }

    private SaleLine line(ProductVariant variant, int quantity) {
        return new SaleLine(variant.getProduct().getId(), variant.getId(), variant.getUnitsPerSale(), quantity);
    }

    private BigDecimal onHand(Product product) {
        return inventoryRepository.findByProductId(product.getId()).map(Inventory::getQuantityOnHand).orElseThrow();
    }

    private BigDecimal batchTotal(Product product) {
        return stockBatchRepository.findByProductIdAndRemainingQuantityGreaterThanOrderByCreatedAtAsc(
                        product.getId(), BigDecimal.ZERO).stream()
                .map(StockBatch::getRemainingQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Category category(String name, CategoryGroup group) {
        Category category = new Category();
        category.setName(name);
        category.setCategoryGroup(group);
        category = categoryRepository.save(category);
        categories.add(category);
        return category;
    }

    private Product product(String name, Category category, StockUnit stockUnit, SellUnit sellUnit, int unitsPerStock) {
        Product product = new Product();
        product.setName(name);
        product.setSku("IT-" + UUID.randomUUID());
        product.setCategory(category);
        product.setStockUnit(stockUnit);
        product.setSellUnit(sellUnit);
        product.setUnitsPerStock(BigDecimal.valueOf(unitsPerStock));
        product = productRepository.save(product);
        products.add(product);
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventoryRepository.save(inventory);
        return product;
    }

    private ProductVariant variant(Product product, VariantLabel label, String price, int unitsPerSale) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setName(label);
        variant.setPrice(new BigDecimal(price));
        variant.setUnitsPerSale(unitsPerSale);
        return variantRepository.save(variant);
    }
}
