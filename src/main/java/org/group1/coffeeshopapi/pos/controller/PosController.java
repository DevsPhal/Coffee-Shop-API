package org.group1.coffeeshopapi.pos.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.pos.dto.response.PosCatalogItemResponse;
import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.pos.service.PosService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<List<PosCatalogItemResponse>>> getProducts() {
        return ResponseEntity.ok(ApiResponse.<List<PosCatalogItemResponse>>builder().status(HttpStatus.OK.value()).message("POS products retrieved successfully").data(posService.getProducts()).timeStamp(LocalDateTime.now()).build());
    }
}