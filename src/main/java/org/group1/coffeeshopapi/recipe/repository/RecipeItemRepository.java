package org.group1.coffeeshopapi.recipe.repository;

import org.group1.coffeeshopapi.recipe.entity.RecipeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RecipeItemRepository extends JpaRepository<RecipeItem, UUID> {

    @Query("select r from RecipeItem r join fetch r.ingredient where r.variant.id in :variantIds")
    List<RecipeItem> findByVariantIdIn(Collection<UUID> variantIds);

    @Query("select r from RecipeItem r join fetch r.ingredient join fetch r.variant v "
            + "where v.product.id = :productId order by v.sortOrder, v.name, r.ingredient.name")
    List<RecipeItem> findByProductId(UUID productId);

    boolean existsByIngredientId(UUID ingredientId);

    @Modifying
    @Query("delete from RecipeItem r where r.variant.id = :variantId")
    void deleteByVariantId(UUID variantId);

    @Modifying
    @Query("delete from RecipeItem r where r.variant.id in (select v.id from ProductVariant v where v.product.id = :productId)")
    void deleteByProductId(UUID productId);
}
