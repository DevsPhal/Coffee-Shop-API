package org.group1.coffeeshopapi.pos.controller;

import java.util.List;

import org.group1.coffeeshopapi.pos.dto.response.PosCatalogItemResponse;
import org.group1.coffeeshopapi.pos.service.PosService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pos")
@PreAuthorize("hasAnyRole('ADMIN','BARISTA')")
@RequiredArgsConstructor
public class PosController {

    private final PosService posService;

    @GetMapping("/products")
    public List<PosCatalogItemResponse> getProducts() {
        return posService.getProducts();
    }
}
