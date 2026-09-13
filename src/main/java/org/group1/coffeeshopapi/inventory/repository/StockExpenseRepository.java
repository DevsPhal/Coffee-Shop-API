package org.group1.coffeeshopapi.inventory.repository;

import org.group1.coffeeshopapi.inventory.entity.StockExpense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface StockExpenseRepository extends JpaRepository<StockExpense, UUID> {
    Page<StockExpense> findByProductIdOrderByExpenseDateDesc(UUID productId, Pageable pageable);

    // Half-open range: [start, end).
    List<StockExpense> findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(LocalDate start, LocalDate end);
}
