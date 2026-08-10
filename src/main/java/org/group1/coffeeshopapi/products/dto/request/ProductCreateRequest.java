package org.group1.coffeeshopapi.products.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductCreateRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Category code is required")
    private String categoryCode;

    @NotBlank(message = "Unit code is required")
    private String unitCode;

    @NotNull(message = "Price in Riel is required")
    @Positive(message = "Price in Riel must be positive")
    private BigDecimal priceRiel;

    @NotNull(message = "Price in Dollar is required")
    @Positive(message = "Price in Dollar must be positive")
    private BigDecimal priceDollar;

    private String description;

    private String imageUrl;

    private Boolean isActive = true;
}
