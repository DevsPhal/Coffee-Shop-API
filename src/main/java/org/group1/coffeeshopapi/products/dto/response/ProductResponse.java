package org.group1.coffeeshopapi.products.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponse {

    private Long id;

    private String productId;

    private String name;

    private Long categoryId;

    private String categoryCode;

    private String categoryName;

    private String unitCode;

    private BigDecimal priceRiel;

    private BigDecimal priceDollar;

    private String description;

    private String imageUrl;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
