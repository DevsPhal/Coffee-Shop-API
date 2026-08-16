package org.group1.coffeeshopapi.pos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosItemResponse {
    private UUID id;
    private String name;
    private String sku;
    private BigDecimal price;
    private Boolean active;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
