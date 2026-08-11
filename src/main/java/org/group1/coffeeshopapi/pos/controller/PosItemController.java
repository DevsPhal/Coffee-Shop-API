package org.group1.coffeeshopapi.pos.controller;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.pos.dto.request.PosItemRequest;
import org.group1.coffeeshopapi.pos.dto.response.PosItemResponse;
import org.group1.coffeeshopapi.pos.service.PosItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/pos/items")
@PreAuthorize("hasAnyRole('ADMIN','BARISTA')")
@RequiredArgsConstructor
public class PosItemController {

    private final PosItemService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PosItemResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.<List<PosItemResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("POS items retrieved successfully")
                .data(service.getAll())
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PosItemResponse>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.<List<PosItemResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Active POS items retrieved successfully")
                .data(service.getAllActive())
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PosItemResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<PosItemResponse>builder()
                .status(HttpStatus.OK.value())
                .message("POS item retrieved successfully")
                .data(service.getById(id))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PosItemResponse>> create(@RequestBody PosItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<PosItemResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("POS item created successfully")
                .data(service.create(request))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PosItemResponse>> update(@PathVariable UUID id, @RequestBody PosItemRequest request) {
        return ResponseEntity.ok(ApiResponse.<PosItemResponse>builder()
                .status(HttpStatus.OK.value())
                .message("POS item updated successfully")
                .data(service.update(id, request))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.<Void>builder()
                .status(HttpStatus.NO_CONTENT.value())
                .message("POS item deleted successfully")
                .timeStamp(LocalDateTime.now())
                .build());
    }
}
