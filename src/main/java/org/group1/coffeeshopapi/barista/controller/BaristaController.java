package org.group1.coffeeshopapi.barista.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.barista.dto.request.BaristaRequest;
import org.group1.coffeeshopapi.barista.dto.response.BaristaResponse;
import org.group1.coffeeshopapi.barista.service.BaristaService;
import org.group1.coffeeshopapi.common.responses.ApiResponse;
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
@RequestMapping("/api/v1/barista")
@PreAuthorize("hasAnyRole('ADMIN','BARISTA')")
@RequiredArgsConstructor
public class BaristaController {

    private final BaristaService baristaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BaristaResponse>>> getAllBaristas() {
        return ResponseEntity.ok(ApiResponse.<List<BaristaResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Baristas retrieved successfully")
                .data(baristaService.getAllBaristas())
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BaristaResponse>> getBaristaById(@PathVariable Long id) {
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
    public ResponseEntity<ApiResponse<BaristaResponse>> updateBarista(@PathVariable Long id, @RequestBody BaristaRequest request) {
        return ResponseEntity.ok(ApiResponse.<BaristaResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Barista updated successfully")
                .data(baristaService.updateBarista(id, request))
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBarista(@PathVariable Long id) {
        baristaService.deleteBarista(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Barista deleted successfully")
                .timeStamp(LocalDateTime.now())
                .build());
    }
}
