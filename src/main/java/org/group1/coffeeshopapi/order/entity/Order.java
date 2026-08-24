package org.group1.coffeeshopapi.order.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.barista.entity.Barista;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.user.entity.Customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Either a POS sale rung up by a barista ({@code barista} set, {@code customer} null) or a
 * self-service order placed by a customer ({@code customer} set). A customer order that chooses
 * cash-on-pickup keeps {@code customer} and stays {@link OrderStatus#PENDING} with
 * {@code paymentMethod} = CASH until a barista collects the cash in person, at which point
 * {@code barista} is also set on the same order (who placed it vs. who fulfilled it).
 * <p>
 * {@code barista}/{@code customer} are real relations (never the Super Admin, who never touches
 * an order — see SecurityConfig) rather than a plain id + lookup, unlike the audit-style
 * {@code createdBy}/{@code performedBy} fields elsewhere in the system: those can legitimately be
 * either an Admin row or the config-driven Super Admin, which has no backing row to relate to.
 * <p>
 * Stock is only cut from inventory once the order reaches {@link OrderStatus#COMPLETED}
 * (payment confirmed) — a still-{@code PENDING} order that gets cancelled never touched
 * inventory, so cancellation needs no restock logic.
 */
@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barista_id")
    private Barista barista;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    @Column(precision = 12, scale = 2)
    private BigDecimal amountTendered;

    @Column(precision = 12, scale = 2)
    private BigDecimal changeDue;

    @Column(columnDefinition = "TEXT")
    private String bakongQrString;

    @Column(length = 64)
    private String bakongMd5Hash;

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private Currency bakongCurrency;

    // The amount actually encoded in the QR, in bakongCurrency — differs from totalAmount (always
    // USD) when bakongCurrency is KHR, since that's converted via bakong.khr-per-usd-rate.
    @Column(precision = 15, scale = 2)
    private BigDecimal bakongAmount;

    @Column(length = 128)
    private String bakongTransactionHash;

    @Column
    private String note;

    @Column
    private LocalDateTime paidAt;

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
