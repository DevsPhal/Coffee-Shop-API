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

    // The update-time duplicate check: excludes the product's own row, so re-saving it with the
    // same SKU it already has isn't flagged as a clash with itself.
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, UUID id);
    boolean existsByCategoryId(UUID categoryId);
    Page<Product> findByCategoryId(UUID categoryId, Pageable pageable);

    Page<Product> findByStatus(Status status, Pageable pageable);
    Page<Product> findByCategoryIdAndStatus(UUID categoryId, Status status, Pageable pageable);

    // Small-catalog reads for the Telegram bot menu — no pagination needed for a shop's full menu.
    List<Product> findByStatusOrderByNameAsc(Status status);
    List<Product> findByCategoryIdAndStatusOrderByNameAsc(UUID categoryId, Status status);

    // "In stock" (quantityOnHand > 0) can't be a derived query method since it compares against
    // a field on the related Inventory row, not Product itself — hence the explicit JOIN. Backs
    // every customer-facing catalog read (REST and Telegram alike, see ProductServiceImpl
    // #listActive / TelegramCatalogServiceImpl): a customer shouldn't see, let alone be able to
    // add to cart, a product with nothing to sell. The admin catalog (ProductServiceImpl#list)
    // deliberately doesn't use these — staff still need to see and restock a depleted product.
    @Query("SELECT p FROM Product p JOIN Inventory i ON i.product = p " +
            "WHERE p.status = :status AND i.quantityOnHand > 0")
    Page<Product> findByStatusAndInStock(@Param("status") Status status, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN Inventory i ON i.product = p " +
            "WHERE p.category.id = :categoryId AND p.status = :status AND i.quantityOnHand > 0")
    Page<Product> findByCategoryIdAndStatusAndInStock(
            @Param("categoryId") UUID categoryId, @Param("status") Status status, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN Inventory i ON i.product = p " +
            "WHERE p.status = :status AND i.quantityOnHand > 0 ORDER BY p.name ASC")
    List<Product> findByStatusAndInStockOrderByNameAsc(@Param("status") Status status);

    @Query("SELECT p FROM Product p JOIN Inventory i ON i.product = p " +
            "WHERE p.category.id = :categoryId AND p.status = :status AND i.quantityOnHand > 0 ORDER BY p.name ASC")
    List<Product> findByCategoryIdAndStatusAndInStockOrderByNameAsc(
            @Param("categoryId") UUID categoryId, @Param("status") Status status);
}
