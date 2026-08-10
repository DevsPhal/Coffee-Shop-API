package org.group1.coffeeshopapi.payments.service;

import java.util.UUID;

import java.util.List;

import org.group1.coffeeshopapi.payments.dto.request.PaymentCreateRequest;
import org.group1.coffeeshopapi.payments.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse createPayment(PaymentCreateRequest request);
    List<PaymentResponse> getAllPayments();
    PaymentResponse getById(UUID id);
    PaymentResponse getByOrderId(UUID orderId);
    PaymentResponse checkStatus(UUID id);
    PaymentResponse verify(UUID id);
    void deletePayment(UUID id);
}
