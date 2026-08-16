package org.group1.coffeeshopapi.payments.controller;

import java.util.List;
import java.util.UUID;

import org.group1.coffeeshopapi.payments.dto.request.PaymentCreateRequest;
import org.group1.coffeeshopapi.payments.dto.response.PaymentResponse;
import org.group1.coffeeshopapi.payments.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public PaymentResponse createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        return paymentService.createPayment(request);
    }

    @GetMapping
    public List<PaymentResponse> getAllPayments() {
        return paymentService.getAllPayments();
    }

    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable UUID id) {
        return paymentService.getById(id);
    }

    @GetMapping("/order/{orderId}")
    public PaymentResponse getByOrder(@PathVariable UUID orderId) {
        return paymentService.getByOrderId(orderId);
    }

    @PostMapping("/{id}/check-status")
    public PaymentResponse checkStatus(@PathVariable UUID id) {
        return paymentService.checkStatus(id);
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse verify(@PathVariable UUID id) {
        return paymentService.verify(id);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePayment(@PathVariable UUID id) {
        paymentService.deletePayment(id);
    }
}
