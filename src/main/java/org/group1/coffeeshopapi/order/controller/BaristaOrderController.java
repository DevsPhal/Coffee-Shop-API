package org.group1.coffeeshopapi.order.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.group1.coffeeshopapi.order.dto.request.CashPaymentRequest;
import org.group1.coffeeshopapi.order.dto.request.CreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.response.BakongQrResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/barista/orders")
@RequiredArgsConstructor
@Tag(name = "Barista Orders", description = "Barista only: ring up sales and accept cash/Bakong payment")
@SecurityRequirement(name = "bearerAuth")
public class BaristaOrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        OrderResponse response = orderService.create(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Order created successfully.", response));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listOwn(currentUser.getId(), status, PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getById(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, orderService.getOwn(id, currentUser.getId()));
    }

    @PostMapping("/{id}/pay/cash")
    public ApiResponse<OrderResponse> payCash(
            @PathVariable UUID id,
            @Valid @RequestBody CashPaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        OrderResponse response = orderService.payCash(id, currentUser.getId(), request);
        return ApiResponse.of(HttpStatus.OK, "Cash payment recorded successfully.", response);
    }

    @PostMapping("/{id}/pay/bakong/qr")
    public ApiResponse<BakongQrResponse> generateBakongQr(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Bakong KHQR generated successfully.",
                orderService.generateBakongQr(id, currentUser.getId()));
    }

    @PostMapping("/{id}/pay/bakong/confirm")
    public ApiResponse<OrderResponse> confirmBakongPayment(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                orderService.confirmBakongPayment(id, currentUser.getId()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancel(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Order cancelled successfully.",
                orderService.cancel(id, currentUser.getId()));
    }

    // Collects cash in person for a customer's cash-on-pickup order (an order the barista didn't create).
    @PostMapping("/{id}/collect-cash")
    public ApiResponse<OrderResponse> collectCash(
            @PathVariable UUID id,
            @Valid @RequestBody CashPaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        OrderResponse response = orderService.collectCash(id, currentUser.getId(), request);
        return ApiResponse.of(HttpStatus.OK, "Cash collected successfully.", response);
    }
}
