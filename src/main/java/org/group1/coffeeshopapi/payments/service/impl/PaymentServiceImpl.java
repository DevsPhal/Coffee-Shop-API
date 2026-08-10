package org.group1.coffeeshopapi.payments.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.PaymentStatus;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidPaymentException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.exception.UnauthorizedException;
import org.group1.coffeeshopapi.common.security.SecurityUtils;
import org.group1.coffeeshopapi.orders.entity.Order;
import org.group1.coffeeshopapi.orders.repository.OrderRepository;
import org.group1.coffeeshopapi.payments.bakong.BakongApiClient;
import org.group1.coffeeshopapi.payments.bakong.BakongKhqrBuilder;
import org.group1.coffeeshopapi.payments.bakong.BakongTransactionStatus;
import org.group1.coffeeshopapi.payments.bakong.KhqrGenerationResult;
import org.group1.coffeeshopapi.payments.dto.request.PaymentCreateRequest;
import org.group1.coffeeshopapi.payments.dto.response.PaymentResponse;
import org.group1.coffeeshopapi.payments.entity.Payment;
import org.group1.coffeeshopapi.payments.mapper.PaymentMapper;
import org.group1.coffeeshopapi.payments.repository.PaymentRepository;
import org.group1.coffeeshopapi.payments.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;
    private final BakongKhqrBuilder bakongKhqrBuilder;
    private final BakongApiClient bakongApiClient;

    @Override
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

        if (!SecurityUtils.isStaff() && !Objects.equals(order.getUserId(), SecurityUtils.currentUserId())) {
            throw new UnauthorizedException("You do not have access to this order");
        }
        if (request.getMethod() != PaymentMethod.BAKONG_KHQR && !SecurityUtils.isStaff()) {
            throw new InvalidPaymentException("This payment method must be confirmed by staff at the counter");
        }

        if (paymentRepository.existsByOrderId(order.getId())) {
            throw new DuplicateResourceException("A payment already exists for order " + order.getId());
        }

        String currency = request.getCurrency() == null || request.getCurrency().isBlank()
                ? "USD" : request.getCurrency().toUpperCase();

        Payment payment = Payment.builder()
                .orderId(order.getId())
                .method(request.getMethod())
                .amount(order.getTotalAmount())
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .verified(false)
                .build();

        if (request.getMethod() == PaymentMethod.BAKONG_KHQR) {
            KhqrGenerationResult khqr = bakongKhqrBuilder.generate(
                    BigDecimal.valueOf(order.getTotalAmount()), currency, order.getOrderNumber());
            payment.setQrString(khqr.qrString());
            payment.setMd5Hash(khqr.md5Hash());
        } else {
            // Cash is settled at the counter — mark it paid immediately.
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setVerified(true);
        }

        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return paymentMapper.toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));
        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse checkStatus(Long id) {
        Payment payment = findById(id);

        if (payment.getMethod() != PaymentMethod.BAKONG_KHQR) {
            throw new InvalidPaymentException("Only Bakong KHQR payments can be checked against Bakong");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return paymentMapper.toResponse(payment);
        }

        BakongTransactionStatus result = bakongApiClient.checkTransactionByMd5(payment.getMd5Hash());
        if (result.paid()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setVerified(true);
            payment.setBakongTransactionHash(result.transactionHash());
            paymentRepository.save(payment);
        }

        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse verify(Long id) {
        Payment payment = findById(id);
        payment.setVerified(true);
        payment.setStatus(PaymentStatus.SUCCESS);
        return paymentMapper.toResponse(paymentRepository.save(payment));
    }

    @Override
    public void deletePayment(Long id) {
        Payment payment = findById(id);
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new InvalidPaymentException("Cannot delete a payment that has already succeeded");
        }
        paymentRepository.delete(payment);
    }

    private Payment findById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }
}
