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
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.FulfillmentMethod;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.user.entity.Customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// A staff-rung POS sale (handledBy set, no customer) or a customer's own order (customer set).
// A cash order can start being prepared before it's actually paid, so paidAt — not status — is
// the real signal for whether it's been paid.
@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    // The admin or barista who rang up, collected payment for, or served this order — null until
    // one of them does.
    @Column
    private UUID handledBy;

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
    private FulfillmentMethod fulfillmentMethod = FulfillmentMethod.PICKUP;

    // Set by staff once they evaluate the delivery — zero until then, even for a delivery order.
    @Column(precision = 12, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    // When staff last quoted the delivery fee. Null means not quoted yet, which is how a real
    // zero fee (free delivery) is told apart from "still waiting".
    private LocalDateTime deliveryFeeSetAt;

    @Column(length = 500)
    private String deliveryAddress;

    @Column(length = 120)
    private String contactName;

    @Column(length = 30)
    private String contactPhone;

    // Stamped when a delivery order leaves the shop, and when the courier confirms it arrived.
    // Both stay null for a pickup order, which never enters the delivery leg.
    private LocalDateTime dispatchedAt;

    private LocalDateTime deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    // How much cash was actually handed over, in whichever currency the customer paid with.
    @Column(precision = 15, scale = 2)
    private BigDecimal amountTendered;

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private Currency amountTenderedCurrency;

    // Change given back, in changeCurrency below — normally the same currency as
    // amountTenderedCurrency, unless the customer asked for the other one.
    @Column(precision = 15, scale = 2)
    private BigDecimal changeDue;

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private Currency changeCurrency;

    @Column(columnDefinition = "TEXT")
    private String bakongQrString;

    @Column(length = 64)
    private String bakongMd5Hash;

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private Currency bakongCurrency;

    // The amount encoded in the QR, in bakongCurrency — differs from totalAmount (always USD)
    // when bakongCurrency is KHR.
    @Column(precision = 15, scale = 2)
    private BigDecimal bakongAmount;

    @Column(length = 128)
    private String bakongTransactionHash;

    @Column
    private LocalDateTime bakongExpiresAt;

    // Free-text note from the customer/barista. TEXT rather than varchar(255) since a delivery
    // address plus a note can easily run long.
    @Column(columnDefinition = "TEXT")
    private String note;

    @Column
    private LocalDateTime paidAt;

    // The GPS pin the customer dropped at checkout — null for a pickup order. Both set together
    // or not at all.
    @Column(precision = 9, scale = 6)
    private BigDecimal deliveryLatitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal deliveryLongitude;

    // True if either fulfillmentMethod or a pinned GPS location says this is a delivery.
    public boolean isDelivery() {
        return fulfillmentMethod == FulfillmentMethod.DELIVERY || (deliveryLatitude != null && deliveryLongitude != null);
    }

    // A pending delivery order that can't be paid or prepared yet, because staff hasn't quoted
    // its fee. Only PENDING counts, since the fee can't be set after that.
    public boolean isAwaitingDeliveryFee() {
        return status == OrderStatus.PENDING && isDelivery() && deliveryFeeSetAt == null;
    }

    // Sum of the line subtotals, before the delivery fee.
    public BigDecimal getItemsTotal() {
        return items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
