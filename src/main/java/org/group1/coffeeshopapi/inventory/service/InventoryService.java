package org.group1.coffeeshopapi.inventory.service;

import org.group1.coffeeshopapi.inventory.dto.request.MovementSearch;
import org.group1.coffeeshopapi.inventory.dto.request.SaleLine;
import org.group1.coffeeshopapi.inventory.dto.request.StockCountRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockCutRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockInRequest;
import org.group1.coffeeshopapi.inventory.dto.request.StockReturnRequest;
import org.group1.coffeeshopapi.inventory.dto.request.WasteRequest;
import org.group1.coffeeshopapi.inventory.dto.response.InventoryResponse;
import org.group1.coffeeshopapi.inventory.dto.response.StockCutResponse;
import org.group1.coffeeshopapi.inventory.dto.response.StockMovementResponse;
import org.group1.coffeeshopapi.product.entity.ProductVariant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Every change to stock goes through here, so on-hand quantities, batches and the movement history always agree.
public interface InventoryService {
    InventoryResponse getByProduct(UUID productId);
    Page<InventoryResponse> list(Pageable pageable);

    Page<InventoryResponse> listLowStock(Pageable pageable);

    StockMovementResponse stockIn(StockInRequest request, UUID performedBy);
    StockCutResponse stockCut(StockCutRequest request, UUID performedBy);

    StockCutResponse recordWaste(WasteRequest request, UUID performedBy);

    StockMovementResponse returnToStock(StockReturnRequest request, UUID performedBy);

    StockMovementResponse adjustToCount(StockCountRequest request, UUID performedBy);

    // Throws when an order can't be filled: not enough of a product, or of an ingredient its recipe uses.
    void requireAvailableForSale(List<SaleLine> lines);

    // Takes a paid order out of stock once. capToAvailable cuts what is there instead of failing (money already taken).
    void deductForSale(UUID orderId, List<SaleLine> lines, UUID performedBy, boolean capToAvailable);

    Map<UUID, VariantStock> variantStock(Collection<ProductVariant> variants);

    // Cost of one base unit: the stock on hand averaged over its batches, else the last purchase, else 0.
    Map<UUID, BigDecimal> averageUnitCosts(Collection<UUID> productIds);

    Page<StockMovementResponse> listMovements(UUID productId, Pageable pageable);

    Page<StockMovementResponse> searchMovements(MovementSearch search, Pageable pageable);
}
