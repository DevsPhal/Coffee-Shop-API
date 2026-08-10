package org.group1.coffeeshopapi.payments.service;

import java.util.List;

import org.group1.coffeeshopapi.payments.dto.request.PaymentCreateRequest;
import org.group1.coffeeshopapi.payments.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse createPayment(PaymentCreateRequest request);
    List<PaymentResponse> getAllPayments();
    PaymentResponse getById(Long id);
    PaymentResponse getByOrderId(Long orderId);
    PaymentResponse checkStatus(Long id);
    PaymentResponse verify(Long id);
    void deletePayment(Long id);
}
