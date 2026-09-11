package org.group1.coffeeshopapi.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.product.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDate;

// The cost of a single stock-in event ("money out" for restocking). Auto-generated only — see
// InventoryServiceImpl.recordStockPurchaseExpense — never created or edited by an admin directly,
// so there's no staff/attendant field here: who performed the stock-in is already on the linked
// StockMovement.performedBy, and duplicating it would just drift out of sync.
@Getter
@Setter
@Entity
@Table(name = "stock_expenses")
public class StockExpense extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_movement_id", nullable = false)
    private StockMovement stockMovement;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate expenseDate;
}
