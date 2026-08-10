package org.group1.coffeeshopapi.barista.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.group1.coffeeshopapi.barista.dto.request.BaristaRequest;
import org.group1.coffeeshopapi.barista.dto.response.BaristaResponse;
import org.group1.coffeeshopapi.barista.service.BaristaService;
import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.common.responses.PageResponse;
import org.group1.coffeeshopapi.common.utils.PageUtil;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/barista")
@PreAuthorize("hasAnyRole('ADMIN','BARISTA')")
@RequiredArgsConstructor
public class BaristaController {

    private final BaristaService baristaService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BaristaResponse>>> getAllBaristas(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        Pageable pageable = PageUtil.buildPageable(page, size, sortBy, direction);
        return ResponseEntity.ok(ApiResponse.<PageResponse<BaristaResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Baristas retrieved successfully")
                .data(baristaService.getAllBaristas(pageable))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BaristaResponse>> getBaristaById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<BaristaResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Barista retrieved successfully")
                .data(baristaService.getBaristaById(id))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BaristaResponse>> createBarista(@RequestBody BaristaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<BaristaResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Barista created successfully")
                .data(baristaService.createBarista(request))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BaristaResponse>> updateBarista(@PathVariable UUID id, @RequestBody BaristaRequest request) {
        return ResponseEntity.ok(ApiResponse.<BaristaResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Barista updated successfully")
                .data(baristaService.updateBarista(id, request))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBarista(@PathVariable UUID id) {
        baristaService.deleteBarista(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Barista deleted successfully")
                .timeStamp(LocalDateTime.now())
                .build());
    }
}
