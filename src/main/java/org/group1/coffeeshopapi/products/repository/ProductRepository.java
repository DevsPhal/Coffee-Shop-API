package org.group1.coffeeshopapi.products.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.group1.coffeeshopapi.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByProductId(String productId);

    boolean existsByCategory_Id(UUID categoryId);

    @Query("SELECT p FROM Product p WHERE p.isActive = true")
    List<Product> findAllActive();

    @Query("SELECT p FROM Product p WHERE p.category.code = :categoryCode AND p.isActive = true")
    List<Product> findByCategoryCodeAndActive(@Param("categoryCode") String categoryCode);

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) AND p.isActive = true")
    List<Product> findByNameContainingIgnoreCaseAndActive(@Param("name") String name);
}
