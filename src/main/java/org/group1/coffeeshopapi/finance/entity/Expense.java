package org.group1.coffeeshopapi.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

// A manually logged cash outflow (rent, supplies, wages, ...) — "money out" for profit reporting.
@Getter
@Setter
@Entity
@Table(name = "expenses")
public class Expense extends BaseEntity {

    @Column(nullable = false)
    private String category;

    @Column
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate expenseDate;

    @Column(nullable = false)
    private UUID recordedBy;
}
