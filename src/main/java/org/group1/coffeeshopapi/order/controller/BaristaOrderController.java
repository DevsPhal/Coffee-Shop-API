package org.group1.coffeeshopapi.order.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.common.constant.AppConstant;
import org.group1.coffeeshopapi.common.enums.Currency;
import org.group1.coffeeshopapi.common.enums.OrderStatus;
import org.group1.coffeeshopapi.common.exception.InvalidOperationException;
import org.group1.coffeeshopapi.common.response.ApiResponse;
import org.group1.coffeeshopapi.common.response.PageResponse;
import org.group1.coffeeshopapi.common.security.CustomUserDetails;
import org.group1.coffeeshopapi.common.util.FileResponseUtil;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.group1.coffeeshopapi.common.util.QrImageUtil;
import org.group1.coffeeshopapi.order.dto.request.CashPaymentRequest;
import org.group1.coffeeshopapi.order.dto.request.CreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.request.DeliveryFeeRequest;
import org.group1.coffeeshopapi.order.dto.response.BakongQrResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.order.service.ReceiptService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    // A 300x300 PNG scans reliably on a phone camera without being needlessly large to transfer.
    private static final int QR_IMAGE_SIZE = 300;

    private final OrderService orderService;
    private final ReceiptService receiptService;

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

    // The printable receipt for a walk-in sale this barista rang up/collected — what they hand
    // (or print) to the customer in person once the order is COMPLETED. Scoped to this barista's
    // own orders the same way getById above is; getOwn throws if they didn't handle it.
    @GetMapping(value = "/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getReceipt(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        orderService.getOwn(id, currentUser.getId());
        byte[] pdf = receiptService.generateReceiptPdf(id);
        return FileResponseUtil.respond(pdf, MediaType.APPLICATION_PDF, "receipt-" + id + ".pdf", true);
    }

    // Visibility into every order in the system, not just ones this barista created or already
    // collected — so a barista can find a customer's pending cash-on-pickup order to accept via
    // collectCash below. OrderResponse.handledById/handledByName already show whether an order is
    // still unclaimed (null) or has been handled by this barista, an admin, or a different barista.
    @GetMapping("/all")
    public ApiResponse<PageResponse<OrderResponse>> listAll(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listAll(null, null, status, PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/all/{id}")
    public ApiResponse<OrderResponse> getAny(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, orderService.getAny(id));
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
            @PathVariable UUID id,
            @RequestParam(required = false) Currency currency,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Bakong KHQR generated successfully.",
                orderService.generateBakongQr(id, currentUser.getId(), currency));
    }

    @PostMapping("/{id}/pay/bakong/confirm")
    public ApiResponse<OrderResponse> confirmBakongPayment(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                orderService.confirmBakongPayment(id, currentUser.getId()));
    }

    // A scannable rendering of the QR string generateBakongQr above already produced (and stored
    // on the order) — that endpoint returns raw payload text a banking app can't scan directly.
    @GetMapping(value = "/{id}/pay/bakong/qr/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getBakongQrImage(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        OrderResponse order = orderService.getOwn(id, currentUser.getId());
        if (order.bakongQrString() == null) {
            throw new InvalidOperationException("No Bakong QR has been generated for this order yet");
        }
        byte[] png = QrImageUtil.toPng(order.bakongQrString(), QR_IMAGE_SIZE);
        return FileResponseUtil.respond(png, MediaType.IMAGE_PNG, "order-" + id + "-qr.png", true);
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancel(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Order cancelled successfully.",
                orderService.cancel(id, currentUser.getId()));
    }

    // The pickup queue: customer cash-on-pickup orders no barista has claimed yet — what a
    // barista browses to find an order to accept via collect-cash below.
    @GetMapping("/awaiting-pickup")
    public ApiResponse<PageResponse<OrderResponse>> listAwaitingPickup(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listAwaitingPickup(PageUtil.buildPageable(page, size))));
    }

    // Collects cash in person for a customer's cash order (pickup or delivery, one the barista
    // didn't create) — whether it's still PENDING or already being prepared/out for delivery
    // (see OrderService#collectCash).
    @PostMapping("/{id}/collect-cash")
    public ApiResponse<OrderResponse> collectCash(
            @PathVariable UUID id,
            @Valid @RequestBody CashPaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        OrderResponse response = orderService.collectCash(id, currentUser.getId(), request);
        return ApiResponse.of(HttpStatus.OK, "Cash collected successfully.", response);
    }

    // The Bakong counterpart to awaiting-pickup: customer orders with a QR generated, still
    // PENDING, that no barista has claimed yet.
    @GetMapping("/awaiting-bakong-confirmation")
    public ApiResponse<PageResponse<OrderResponse>> listAwaitingBakongConfirmation(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listAwaitingBakongConfirmation(PageUtil.buildPageable(page, size))));
    }

    // The Bakong counterpart to collect-cash: confirms/accepts a customer's Bakong-paid order
    // this barista didn't create — checks payment status the same way the customer's own confirm
    // does, and attributes the order to this barista once paid.
    @PostMapping("/{id}/accept-bakong")
    public ApiResponse<OrderResponse> acceptBakongPayment(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        OrderResponse response = orderService.acceptBakongPayment(id, currentUser.getId());
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, response);
    }

    // Sets (or revises) the delivery fee for a customer's delivery order — see
    // OrderResponse.deliveryLatitude/deliveryLongitude/distanceMeters for what a barista has to
    // go on when evaluating it. Immediately reflected in totalAmount. Not scoped to one this
    // barista has claimed — any still-pending delivery order can be evaluated.
    @PostMapping("/{id}/delivery-fee")
    public ApiResponse<OrderResponse> setDeliveryFee(
            @PathVariable UUID id, @Valid @RequestBody DeliveryFeeRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        OrderResponse response = orderService.setDeliveryFee(id, request.fee(), currentUser.getId());
        return ApiResponse.of(HttpStatus.OK, "Delivery fee set successfully.", response);
    }

    // The kitchen queue: orders ready to start on right now — PAID ones, plus a customer's cash
    // order that hasn't been paid yet but is fair game anyway (see startPreparing) — what a
    // barista browses to find one to start via prepare below.
    @GetMapping("/awaiting-preparation")
    public ApiResponse<PageResponse<OrderResponse>> listAwaitingPreparation(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listAwaitingPreparation(PageUtil.buildPageable(page, size))));
    }

    // PAID -> PREPARING, or (cash only) PENDING -> PREPARING — cash can be collected up front or
    // at handover, so a cash order doesn't have to wait for payment to be started on. Not scoped
    // to who collected payment — any barista can start on any order sitting in the queue above.
    @PostMapping("/{id}/prepare")
    public ApiResponse<OrderResponse> startPreparing(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Order marked as preparing.",
                orderService.startPreparing(id, currentUser.getId()));
    }

    // PREPARING -> COMPLETED — handed to the customer at the counter. Pickup orders only; a
    // delivery order goes through dispatch/deliver below instead. Rejects an unpaid cash order —
    // collect via /collect-cash first.
    @PostMapping("/{id}/complete")
    public ApiResponse<OrderResponse> completePickup(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Order completed successfully.",
                orderService.completePickup(id, currentUser.getId()));
    }

    // PREPARING -> OUT_FOR_DELIVERY — the order has left the shop with a courier. Delivery orders
    // only.
    @PostMapping("/{id}/dispatch")
    public ApiResponse<OrderResponse> dispatchForDelivery(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Order dispatched for delivery.",
                orderService.dispatchForDelivery(id, currentUser.getId()));
    }

    // OUT_FOR_DELIVERY -> DELIVERED — the courier confirms it arrived. Rejects an unpaid cash
    // order — collect via /collect-cash first (cash-on-delivery is collected on arrival, before
    // this call).
    @PostMapping("/{id}/deliver")
    public ApiResponse<OrderResponse> markDelivered(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Order marked as delivered.",
                orderService.markDelivered(id, currentUser.getId()));
    }

    // The delivery board: everything currently out with a courier, oldest dispatch first — what
    // a barista browses to find one to confirm via deliver above.
    @GetMapping("/delivery-board")
    public ApiResponse<PageResponse<OrderResponse>> listDeliveryBoard(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listDeliveryBoard(PageUtil.buildPageable(page, size))));
    }
}
