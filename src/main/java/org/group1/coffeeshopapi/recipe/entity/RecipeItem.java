package org.group1.coffeeshopapi.recipe.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.group1.coffeeshopapi.common.entity.BaseEntity;
import org.group1.coffeeshopapi.product.entity.Product;
import org.group1.coffeeshopapi.product.entity.ProductVariant;

import java.math.BigDecimal;

// One ingredient of one size of a drink: selling a Medium Latte uses 18 GRAM of beans.
@Getter
@Setter
@Entity
@Table(name = "recipe_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_recipe_items_variant_ingredient", columnNames = {"variant_id", "ingredient_id"})
})
public class RecipeItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Product ingredient;

    // In the ingredient's base unit (its sell unit), per one sale of the variant.
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;
}
