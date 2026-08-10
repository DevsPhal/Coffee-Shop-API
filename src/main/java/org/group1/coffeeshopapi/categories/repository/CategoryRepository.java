package org.group1.coffeeshopapi.categories.repository;

import java.util.List;
import java.util.Optional;

import org.group1.coffeeshopapi.categories.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCode(String code);

    boolean existsByCode(String code);

    @Query("SELECT c FROM Category c WHERE c.isActive = true")
    List<Category> findAllActive();
}
