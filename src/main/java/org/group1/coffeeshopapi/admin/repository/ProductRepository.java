package org.group1.coffeeshopapi.admin.repository;

import org.group1.coffeeshopapi.admin.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByCategoryId(UUID categoryId);

    boolean existsByNameIgnoreCase(String name);
}