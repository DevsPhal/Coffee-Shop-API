package org.group1.coffeeshopapi.products.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductUpdateRequest {

    private String name;

    private String categoryCode;

    private String unitCode;

    @Positive(message = "Price in Riel must be positive")
    private BigDecimal priceRiel;

    @Positive(message = "Price in Dollar must be positive")
    private BigDecimal priceDollar;

    private String description;

    private String imageUrl;

    private Boolean isActive;
}
