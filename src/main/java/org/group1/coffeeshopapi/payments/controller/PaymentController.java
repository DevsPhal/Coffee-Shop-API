package org.group1.coffeeshopapi.payments.controller;

import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;

import org.group1.coffeeshopapi.common.responses.ApiResponse;
import org.group1.coffeeshopapi.payments.dto.request.PaymentCreateRequest;
import org.group1.coffeeshopapi.payments.dto.response.PaymentResponse;
import org.group1.coffeeshopapi.payments.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<PaymentResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Payment created successfully")
                .data(response)
                .timeStamp(LocalDateTime.now())
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getAllPayments() {
        return ResponseEntity.ok(ApiResponse.<List<PaymentResponse>>builder().status(HttpStatus.OK.value()).message("Payments retrieved successfully").data(paymentService.getAllPayments()).timeStamp(LocalDateTime.now()).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder().status(HttpStatus.OK.value()).message("Payment retrieved successfully").data(paymentService.getById(id)).timeStamp(LocalDateTime.now()).build());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder().status(HttpStatus.OK.value()).message("Payment retrieved successfully").data(paymentService.getByOrderId(orderId)).timeStamp(LocalDateTime.now()).build());
    }

    @PostMapping("/{id}/check-status")
    public ResponseEntity<ApiResponse<PaymentResponse>> checkStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder().status(HttpStatus.OK.value()).message("Payment status checked").data(paymentService.checkStatus(id)).timeStamp(LocalDateTime.now()).build());
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PaymentResponse>> verify(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder().status(HttpStatus.OK.value()).message("Payment verified successfully").data(paymentService.verify(id)).timeStamp(LocalDateTime.now()).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<?>> deletePayment(@PathVariable UUID id) {
        paymentService.deletePayment(id);
        return ResponseEntity.ok(ApiResponse.builder().status(HttpStatus.OK.value()).message("Payment deleted successfully").timeStamp(LocalDateTime.now()).build());
    }
}
