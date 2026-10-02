package org.group1.coffeeshopapi.table.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.common.enums.TableSize;
import org.group1.coffeeshopapi.common.enums.TableStatus;
import org.group1.coffeeshopapi.realtime.ResourceChangeEntityListener;

@Getter
@Setter
@Entity
@Table(name = "dining_tables")
@EntityListeners(ResourceChangeEntityListener.class)
public class DiningTable extends BaseEntity {

    @Column(nullable = false, unique = true, length = 20)
    private String tableNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableSize size = TableSize.SMALL;

    @Column(nullable = false)
    private Integer capacity = TableSize.SMALL.defaultCapacity();

    @Column(nullable = false)
    private Integer guestCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableStatus status = TableStatus.AVAILABLE;
}
