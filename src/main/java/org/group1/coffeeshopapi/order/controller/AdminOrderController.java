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
import org.group1.coffeeshopapi.common.security.CurrentActor;
import org.group1.coffeeshopapi.common.util.FileResponseUtil;
import org.group1.coffeeshopapi.common.util.PageUtil;
import org.group1.coffeeshopapi.common.util.QrImageUtil;
import org.group1.coffeeshopapi.order.dto.request.CashPaymentRequest;
import org.group1.coffeeshopapi.order.dto.request.DeliveryFeeRequest;
import org.group1.coffeeshopapi.order.dto.request.StaffCreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.response.BakongQrResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderAuditLogResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.order.service.ReceiptService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@Tag(name = "Admin Orders", description = "Admin only: ring up walk-in sales the same as a barista can "
        + "(one order, one or many items), view every order (barista and customer), process/serve them, "
        + "and audit who handled what")
@SecurityRequirement(name = "bearerAuth")
public class AdminOrderController {

    // A 300x300 PNG scans reliably on a phone camera without being needlessly large to transfer.
    private static final int QR_IMAGE_SIZE = 300;

    private final OrderService orderService;
    private final ReceiptService receiptService;
    // Every action endpoint here is reachable by the Super Admin too (hasRole("ADMIN") + role
    // hierarchy), whose principal isn't a CustomUserDetails — see CurrentActor's javadoc.
    private final CurrentActor currentActor;

    // Rings up a walk-in sale in person, same as a barista can — one order, carrying one or more
    // items (see StaffCreateOrderRequest.items), always pickup since the customer is standing
    // right there. Reuses OrderService.create/payCash/generateBakongQr/confirmBakongPayment as-is:
    // they're keyed off a plain actor id (Order.handledBy — see its javadoc), not a
    // barista-specific type, so an admin's own id scopes exactly the same way.
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody StaffCreateOrderRequest request) {
        OrderResponse response = orderService.create(request, currentActor.id());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED, "Order created successfully.", response));
    }

    @PostMapping("/{id}/pay/cash")
    public ApiResponse<OrderResponse> payCash(@PathVariable UUID id, @Valid @RequestBody CashPaymentRequest request) {
        OrderResponse response = orderService.payCash(id, currentActor.id(), request);
        return ApiResponse.of(HttpStatus.OK, "Cash payment recorded successfully.", response);
    }

    @PostMapping("/{id}/pay/bakong/qr")
    public ApiResponse<BakongQrResponse> generateBakongQr(
            @PathVariable UUID id, @RequestParam(required = false) Currency currency) {
        return ApiResponse.of(HttpStatus.OK, "Bakong KHQR generated successfully.",
                orderService.generateBakongQr(id, currentActor.id(), currency));
    }

    @PostMapping("/{id}/pay/bakong/confirm")
    public ApiResponse<OrderResponse> confirmBakongPayment(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                orderService.confirmBakongPayment(id, currentActor.id()));
    }

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

    // The printable receipt for any completed order — an admin can produce it the same as
    // whichever barista actually handled the sale.
    @GetMapping(value = "/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getReceipt(@PathVariable UUID id) {
        byte[] pdf = receiptService.generateReceiptPdf(id);
        return FileResponseUtil.respond(pdf, MediaType.APPLICATION_PDF, "receipt-" + id + ".pdf", true);
    }

    // Full audit trail for one order — who created it, who collected/confirmed payment, who
    // cancelled it — since OrderResponse.handledById alone only ever shows the most recent actor.
    @GetMapping("/{id}/history")
    public ApiResponse<List<OrderAuditLogResponse>> getHistory(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, orderService.getHistory(id));
    }

    // A scannable rendering of whatever Bakong QR string is already stored on the order (customer
    // self-checkout, or a barista's or this admin's own walk-up sale via generateBakongQr above)
    // — useful for displaying it again at the register. Unlike generateBakongQr, this just reads
    // whatever's already stored — not scoped to orders this admin rang up themselves.
    @GetMapping(value = "/{id}/pay/bakong/qr/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getBakongQrImage(@PathVariable UUID id) {
        OrderResponse order = orderService.getAny(id);
        if (order.bakongQrString() == null) {
            throw new InvalidOperationException("No Bakong QR has been generated for this order yet");
        }
        byte[] png = QrImageUtil.toPng(order.bakongQrString(), QR_IMAGE_SIZE);
        return FileResponseUtil.respond(png, MediaType.IMAGE_PNG, "order-" + id + "-qr.png", true);
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

    // Collects cash in person for a customer's cash order (pickup or delivery), same as a barista
    // would — whether it's still PENDING or already being prepared/out for delivery (see
    // OrderService#collectCash).
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

    // Cancels any still-pending order (not scoped to one the admin themselves rang up).
    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancel(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order cancelled successfully.",
                orderService.cancelAny(id, currentActor.id()));
    }

    // Sets (or revises) the delivery fee for a customer's delivery order — see
    // OrderResponse.deliveryLatitude/deliveryLongitude/distanceMeters for what an admin has to go
    // on when evaluating it. Immediately reflected in totalAmount. Not scoped to one the admin
    // themselves has claimed — any still-pending delivery order can be evaluated.
    @PostMapping("/{id}/delivery-fee")
    public ApiResponse<OrderResponse> setDeliveryFee(
            @PathVariable UUID id, @Valid @RequestBody DeliveryFeeRequest request) {
        OrderResponse response = orderService.setDeliveryFee(id, request.fee(), currentActor.id());
        return ApiResponse.of(HttpStatus.OK, "Delivery fee set successfully.", response);
    }

    // The kitchen queue: orders ready to start on right now — PAID ones, plus a customer's cash
    // order that hasn't been paid yet but is fair game anyway (see startPreparing) — what an
    // admin browses to find one to start via prepare below.
    @GetMapping("/awaiting-preparation")
    public ApiResponse<PageResponse<OrderResponse>> listAwaitingPreparation(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listAwaitingPreparation(PageUtil.buildPageable(page, size))));
    }

    // PAID -> PREPARING, or (cash only) PENDING -> PREPARING — cash can be collected up front or
    // at handover, so a cash order doesn't have to wait for payment to be started on. Not scoped
    // to who collected payment — any admin/barista can start on any order sitting in the queue
    // above.
    @PostMapping("/{id}/prepare")
    public ApiResponse<OrderResponse> startPreparing(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order marked as preparing.",
                orderService.startPreparing(id, currentActor.id()));
    }

    // PREPARING -> COMPLETED — handed to the customer at the counter. Pickup orders only; a
    // delivery order goes through dispatch/deliver below instead. Rejects an unpaid cash order —
    // collect via /collect-cash first.
    @PostMapping("/{id}/complete")
    public ApiResponse<OrderResponse> completePickup(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order completed successfully.",
                orderService.completePickup(id, currentActor.id()));
    }

    // PREPARING -> OUT_FOR_DELIVERY — the order has left the shop with a courier. Delivery orders
    // only.
    @PostMapping("/{id}/dispatch")
    public ApiResponse<OrderResponse> dispatchForDelivery(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order dispatched for delivery.",
                orderService.dispatchForDelivery(id, currentActor.id()));
    }

    // OUT_FOR_DELIVERY -> DELIVERED — the courier confirms it arrived. Rejects an unpaid cash
    // order — collect via /collect-cash first (cash-on-delivery is collected on arrival, before
    // this call).
    @PostMapping("/{id}/deliver")
    public ApiResponse<OrderResponse> markDelivered(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order marked as delivered.",
                orderService.markDelivered(id, currentActor.id()));
    }

    // The delivery board: everything currently out with a courier, oldest dispatch first — what
    // an admin browses to find one to confirm via deliver above.
    @GetMapping("/delivery-board")
    public ApiResponse<PageResponse<OrderResponse>> listDeliveryBoard(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listDeliveryBoard(PageUtil.buildPageable(page, size))));
    }
}
