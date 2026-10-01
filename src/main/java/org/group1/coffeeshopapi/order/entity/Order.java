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

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

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

    @Column(precision = 12, scale = 2)
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    private LocalDateTime deliveryFeeSetAt;

    @Column(length = 500)
    private String deliveryAddress;

    @Column(length = 120)
    private String contactName;

    @Column(length = 30)
    private String contactPhone;

    private LocalDateTime dispatchedAt;

    private LocalDateTime deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PaymentMethod paymentMethod;

    @Column(precision = 15, scale = 2)
    private BigDecimal amountTendered;

    @Enumerated(EnumType.STRING)
    @Column(length = 3)
    private Currency amountTenderedCurrency;

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

    @Column(precision = 15, scale = 2)
    private BigDecimal bakongAmount;

    @Column(length = 128)
    private String bakongTransactionHash;

    @Column
    private LocalDateTime bakongExpiresAt;

    @Column(length = 1000)
    private String bakongPreviousMd5Hashes;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column
    private LocalDateTime paidAt;

    @Column(precision = 9, scale = 6)
    private BigDecimal deliveryLatitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal deliveryLongitude;

    public boolean isDelivery() {
        return fulfillmentMethod == FulfillmentMethod.DELIVERY || (deliveryLatitude != null && deliveryLongitude != null);
    }

    public boolean isAwaitingDeliveryFee() {
        return status == OrderStatus.PENDING && isDelivery() && deliveryFeeSetAt == null;
    }

    public BigDecimal getItemsTotal() {
        return items.stream().map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
