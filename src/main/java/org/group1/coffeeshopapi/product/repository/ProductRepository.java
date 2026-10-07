package org.group1.coffeeshopapi.product.repository;

import org.group1.coffeeshopapi.common.enums.Status;
import org.group1.coffeeshopapi.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsBySkuIgnoreCase(String sku);
    Optional<Product> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);

    @Query("SELECT p.sku FROM Product p WHERE UPPER(p.sku) LIKE CONCAT(:prefix, '%')")
    List<String> findSkusStartingWith(@Param("prefix") String prefix);
    boolean existsByCategoryId(UUID categoryId);
    boolean existsByNameIgnoreCaseAndCategoryId(String name, UUID categoryId);
    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

    Page<Product> findByStatus(Status status, Pageable pageable);
    Page<Product> findByCategoryIdAndStatus(UUID categoryId, Status status, Pageable pageable);

    List<Product> findByStatusOrderByNameAsc(Status status);
    List<Product> findByCategoryIdAndStatusOrderByNameAsc(UUID categoryId, Status status);

    @Query("SELECT p FROM Product p JOIN Inventory i ON i.product = p " +
            "WHERE p.status = :status AND p.category.status = :status AND i.quantityOnHand > 0")
    Page<Product> findByStatusAndInStock(@Param("status") Status status, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN Inventory i ON i.product = p " +
            "WHERE p.category.id = :categoryId AND p.status = :status AND p.category.status = :status AND i.quantityOnHand > 0")
    Page<Product> findByCategoryIdAndStatusAndInStock(
            @Param("categoryId") UUID categoryId, @Param("status") Status status, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN Inventory i ON i.product = p " +
            "WHERE p.status = :status AND p.category.status = :status AND i.quantityOnHand > 0 ORDER BY p.name ASC")
    List<Product> findByStatusAndInStockOrderByNameAsc(@Param("status") Status status);

    @Query("SELECT p FROM Product p JOIN Inventory i ON i.product = p " +
            "WHERE p.category.id = :categoryId AND p.status = :status AND p.category.status = :status AND i.quantityOnHand > 0 ORDER BY p.name ASC")
    List<Product> findByCategoryIdAndStatusAndInStockOrderByNameAsc(
            @Param("categoryId") UUID categoryId, @Param("status") Status status);
}
