package org.group1.coffeeshopapi.order.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.security.CurrentActor;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.group1.coffeeshopapi.order.dto.request.CashPaymentRequest;
import org.group1.coffeeshopapi.order.dto.response.OrderAuditLogResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin Orders", description = "Admin only: view every order (barista and customer), "
        + "process/serve them the same way a barista can, and audit who handled what")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {

    private final OrderService orderService;
    // Every action endpoint here is reachable by the Super Admin too (hasRole("ADMIN") + role
    // hierarchy), whose principal isn't a CustomUserDetails — see CurrentActor's javadoc.
    private final CurrentActor currentActor;

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> list(
            @RequestParam(required = false) UUID baristaId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listAll(baristaId, customerId, status, PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getById(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, orderService.getAny(id));
    }

    // Full audit trail for one order — who created it, who collected/confirmed payment, who
    // cancelled it — since OrderResponse.handledById alone only ever shows the most recent actor.
    @GetMapping("/{id}/history")
    public ApiResponse<List<OrderAuditLogResponse>> getHistory(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, orderService.getHistory(id));
    }

    // The pickup queue: customer cash-on-pickup orders no admin/barista has claimed yet — what an
    // admin browses to find an order to accept via collect-cash below.
    @GetMapping("/awaiting-pickup")
    public ApiResponse<PageResponse<OrderResponse>> listAwaitingPickup(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listAwaitingPickup(PageUtil.buildPageable(page, size))));
    }

    // Collects cash in person for a customer's cash-on-pickup order, same as a barista would.
    @PostMapping("/{id}/collect-cash")
    public ApiResponse<OrderResponse> collectCash(
            @PathVariable UUID id,
            @Valid @RequestBody CashPaymentRequest request) {
        OrderResponse response = orderService.collectCash(id, currentActor.id(), request);
        return ApiResponse.of(HttpStatus.OK, "Cash collected successfully.", response);
    }

    // The Bakong counterpart to awaiting-pickup: customer orders with a QR generated, still
    // PENDING, that no admin/barista has claimed yet.
    @GetMapping("/awaiting-bakong-confirmation")
    public ApiResponse<PageResponse<OrderResponse>> listAwaitingBakongConfirmation(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listAwaitingBakongConfirmation(PageUtil.buildPageable(page, size))));
    }

    // The Bakong counterpart to collect-cash: confirms/accepts a customer's Bakong-paid order,
    // same as a barista would.
    @PostMapping("/{id}/accept-bakong")
    public ApiResponse<OrderResponse> acceptBakongPayment(@PathVariable UUID id) {
        OrderResponse response = orderService.acceptBakongPayment(id, currentActor.id());
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, response);
    }

    // Cancels any still-unpaid order (not scoped to one the admin themselves rang up).
    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancel(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order cancelled successfully.",
                orderService.cancelAny(id, currentActor.id()));
    }

    // --- Working the queue ---
    // The admin mirror of the barista's start-preparing/complete, so a manager covering the bar
    // can move orders along without a second account. Same service calls, same rules.

    @PostMapping("/{id}/start-preparing")
    public ApiResponse<OrderResponse> startPreparing(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order marked as preparing.",
                orderService.startPreparing(id, currentActor.id()));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<OrderResponse> complete(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order completed.",
                orderService.markCompleted(id, currentActor.id()));
    }

    // The delivery board: orders that have left the shop but are not confirmed as arrived.
    @GetMapping("/out-for-delivery")
    public ApiResponse<PageResponse<OrderResponse>> listOutForDelivery(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listOutForDelivery(PageUtil.buildPageable(page, size))));
    }

    // Hands a delivery order to a courier: it is made, paid for, and now off the premises.
    @PostMapping("/{id}/dispatch")
    public ApiResponse<OrderResponse> dispatch(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order is out for delivery.",
                orderService.markOutForDelivery(id, currentActor.id()));
    }

    // Confirms the courier reached the customer. Terminal for a delivery order.
    @PostMapping("/{id}/delivered")
    public ApiResponse<OrderResponse> delivered(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order marked as delivered.",
                orderService.markDelivered(id, currentActor.id()));
    }

}
