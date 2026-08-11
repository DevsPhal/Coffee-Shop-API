package org.group1.coffeeshopapi.pos.dto.response;

import lombok.Data;

import java.util.UUID;

@Data
public class PosCatalogItemResponse {
    private UUID id;
    private String name;
    private String category;
    private Double price;
    private Integer stock;
    private String unit;
    private String accent;
    private Double discount;
}
