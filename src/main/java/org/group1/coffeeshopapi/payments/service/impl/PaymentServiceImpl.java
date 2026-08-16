package org.group1.coffeeshopapi.payments.service.impl;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.PaymentStatus;
import org.group1.coffeeshopapi.common.exception.DuplicateResourceException;
import org.group1.coffeeshopapi.common.exception.InvalidPaymentException;
import org.group1.coffeeshopapi.common.exception.ResourceNotFoundException;
import org.group1.coffeeshopapi.common.exception.UnauthorizedException;
import org.group1.coffeeshopapi.common.security.SecurityUtils;
import org.group1.coffeeshopapi.orders.entity.Order;
import org.group1.coffeeshopapi.orders.repository.OrderRepository;
import org.group1.coffeeshopapi.payments.dto.request.PaymentCreateRequest;
import org.group1.coffeeshopapi.payments.dto.response.BakongQrResult;
import org.group1.coffeeshopapi.payments.dto.response.BakongTransactionCheckResult;
import org.group1.coffeeshopapi.payments.dto.response.PaymentResponse;
import org.group1.coffeeshopapi.payments.entity.Payment;
import org.group1.coffeeshopapi.payments.mapper.PaymentMapper;
import org.group1.coffeeshopapi.payments.repository.PaymentRepository;
import org.group1.coffeeshopapi.payments.service.BakongPaymentService;
import org.group1.coffeeshopapi.payments.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final String DEFAULT_CURRENCY = "USD";
    private static final EnumSet<PaymentStatus> SUCCESSFUL_STATUSES = EnumSet.of(
            PaymentStatus.SUCCESS,
            PaymentStatus.PAID
    );

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;
    private final BakongPaymentService bakongPaymentService;

    @Override
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        Order order = findOrder(request.getOrderId());
        authorizePaymentCreation(order, request.getMethod());

        PaymentResponse existingPayment = existingPaymentResponseOrReject(order.getId());
        if (existingPayment != null) {
            return existingPayment;
        }

        Payment payment = buildPayment(order, request);
        Payment saved = paymentRepository.save(payment);
        syncOrderAfterSuccessfulPayment(order, saved);

        log.info("Payment after saving: {}", saved.getId());
        return paymentMapper.toResponse(saved);
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
    public PaymentResponse getById(UUID id) {
        return paymentMapper.toResponse(findPayment(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order: " + orderId));
        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse checkStatus(UUID id) {
        Payment payment = findPayment(id);
        validateBakongPayment(payment);

        if (isSuccessful(payment)) {
            return paymentMapper.toResponse(payment);
        }

        BakongTransactionCheckResult result = bakongPaymentService.checkTransactionByMd5(payment.getMd5Hash());
        if (result.paid()) {
            payment = markSuccessful(payment, result.transactionHash());
            syncOrderByPayment(payment);
        }

        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse verify(UUID id) {
        Payment saved = markSuccessful(findPayment(id), null);
        syncOrderByPayment(saved);
        return paymentMapper.toResponse(saved);
    }

    @Override
    public void deletePayment(UUID id) {
        Payment payment = findPayment(id);
        if (isSuccessful(payment)) {
            throw new InvalidPaymentException("Cannot delete a payment that has already succeeded");
        }
        paymentRepository.delete(payment);
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private Payment findPayment(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }

    private void authorizePaymentCreation(Order order, PaymentMethod method) {
        if (!SecurityUtils.isStaff() && !Objects.equals(order.getUserId(), SecurityUtils.currentUserId())) {
            throw new UnauthorizedException("You do not have access to this order");
        }
        if (method != PaymentMethod.BAKONG_KHQR && !SecurityUtils.isStaff()) {
            throw new InvalidPaymentException("This payment method must be confirmed by staff at the counter");
        }
    }

    private PaymentResponse existingPaymentResponseOrReject(UUID orderId) {
        return paymentRepository.findByOrderId(orderId).map(existing -> {
            if (isSuccessful(existing)) {
                return paymentMapper.toResponse(existing);
            }
            throw new DuplicateResourceException("A pending payment already exists for order " + orderId);
        }).orElse(null);
    }

    private Payment buildPayment(Order order, PaymentCreateRequest request) {
        Payment payment = Payment.builder()
                .orderId(order.getId())
                .method(request.getMethod())
                .amount(order.getTotalAmount())
                .currency(resolveCurrency(request.getCurrency()))
                .status(PaymentStatus.PENDING)
                .verified(false)
                .build();

        if (request.getMethod() == PaymentMethod.BAKONG_KHQR) {
            attachBakongKhqr(payment, order);
        } else {
            markSuccessfulInMemory(payment, null);
        }

        return payment;
    }

    private String resolveCurrency(String currency) {
        return currency == null || currency.isBlank() ? DEFAULT_CURRENCY : currency.toUpperCase();
    }

    private void attachBakongKhqr(Payment payment, Order order) {
        BakongQrResult khqr = bakongPaymentService.generateForPayment(
                BigDecimal.valueOf(order.getTotalAmount()),
                payment.getCurrency(),
                order.getOrderNumber()
        );
        payment.setQrString(khqr.qrString());
        payment.setMd5Hash(khqr.md5Hash());
    }

    private void validateBakongPayment(Payment payment) {
        if (payment.getMethod() != PaymentMethod.BAKONG_KHQR) {
            throw new InvalidPaymentException("Only Bakong KHQR payments can be checked against Bakong");
        }
        if (payment.getMd5Hash() == null || payment.getMd5Hash().isBlank()) {
            throw new InvalidPaymentException("Bakong payment is missing an MD5 hash");
        }
    }

    private Payment markSuccessful(Payment payment, String transactionHash) {
        markSuccessfulInMemory(payment, transactionHash);
        return paymentRepository.save(payment);
    }

    private void markSuccessfulInMemory(Payment payment, String transactionHash) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setVerified(true);
        if (transactionHash != null && !transactionHash.isBlank()) {
            payment.setBakongTransactionHash(transactionHash);
        }
    }

    private boolean isSuccessful(Payment payment) {
        return payment.getStatus() != null && SUCCESSFUL_STATUSES.contains(payment.getStatus());
    }

    private void syncOrderByPayment(Payment payment) {
        orderRepository.findById(payment.getOrderId())
                .ifPresent(order -> syncOrderAfterSuccessfulPayment(order, payment));
    }

    private void syncOrderAfterSuccessfulPayment(Order order, Payment payment) {
        if (!isSuccessful(payment)) {
            return;
        }
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);
        }
    }
}
