package org.group1.coffeeshopapi.pos.service;

import java.util.List;

import org.group1.coffeeshopapi.pos.dto.response.PosCatalogItemResponse;

public interface PosService {
    List<PosCatalogItemResponse> getProducts();
}
