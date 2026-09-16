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
import org.group1.coffeeshopapi.common.enums.IceLevel;
import org.group1.coffeeshopapi.common.enums.MilkType;
import org.group1.coffeeshopapi.common.enums.SugarLevel;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Snapshot of the product name at sale time, so the order stays readable if the product is later renamed.
    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    // Snapshot of the product's final (post-discount) price at sale time, size add-on included.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    // Live relation to the chosen variant — same as CartItem.variant. Optional, since not
    // every product is a customizable drink. Unlike productName, this is not also snapshotted as
    // a string: a variant can't be deleted while any order still references it (a delete
    // attempt is rejected — see GlobalExceptionHandler's DataIntegrityViolationException handler),
    // so the relation can't dangle; renaming one does change how past orders display it. Column
    // stays "size_option_id" — see ProductVariant's javadoc on why the Java-side rename doesn't
    // touch physical schema.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_option_id")
    private ProductVariant variant;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SugarLevel sugarLevel;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private IceLevel iceLevel;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MilkType milkType;

    // Extras (e.g. Pearl) the customer added — snapshotted per row, same reasoning as productName
    // above. unitPrice/subtotal already include each extra's price (see OrderServiceImpl.buildOrder).
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemExtra> extras = new ArrayList<>();

    public void addExtra(OrderItemExtra extra) {
        extras.add(extra);
        extra.setOrderItem(this);
    }
}
