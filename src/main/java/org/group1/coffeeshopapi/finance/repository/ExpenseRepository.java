package org.group1.coffeeshopapi.finance.repository;

import org.group1.coffeeshopapi.finance.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {
    Page<Expense> findAllByOrderByExpenseDateDesc(Pageable pageable);

    // Half-open range: [start, end).
    List<Expense> findByExpenseDateGreaterThanEqualAndExpenseDateLessThan(LocalDate start, LocalDate end);
}
