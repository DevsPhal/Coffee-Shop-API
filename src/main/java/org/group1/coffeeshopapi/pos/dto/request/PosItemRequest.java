package org.group1.coffeeshopapi.pos.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosItemRequest {
    private String name;
    private String sku;
    private BigDecimal price;
    private Boolean active;
    private String description;
}
