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
import org.group1.coffeeshopapi.order.dto.request.DeliveryFeeRequest;
import org.group1.coffeeshopapi.order.dto.request.StaffCreateOrderRequest;
import org.group1.coffeeshopapi.order.dto.response.BakongQrResponse;
import org.group1.coffeeshopapi.order.dto.response.OrderResponse;
import org.group1.coffeeshopapi.order.dto.response.StaffCallResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.order.service.ReceiptService;
import org.group1.coffeeshopapi.order.service.StaffCallService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    private final StaffCallService staffCallService;

    // A walk-in sale rung up at the counter. Always pickup, served on the spot.
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @Valid @RequestBody StaffCreateOrderRequest request,
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

    // The printable receipt for a finished order this barista handled. getOwn throws otherwise.
    @GetMapping(value = "/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getReceipt(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        orderService.getOwn(id, currentUser.getId());
        byte[] pdf = receiptService.generateReceiptPdf(id);
        return FileResponseUtil.respond(pdf, MediaType.APPLICATION_PDF, "receipt-" + id + ".pdf", true);
    }

    // Same document, but available as soon as the order is paid.
    @GetMapping(value = "/{id}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getInvoice(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        orderService.getOwn(id, currentUser.getId());
        byte[] pdf = receiptService.generateInvoicePdf(id);
        return FileResponseUtil.respond(pdf, MediaType.APPLICATION_PDF, "invoice-" + id + ".pdf", true);
    }

    // Visibility into every order in the system, not just this barista's own.
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

    // Renders the QR string generateBakongQr already produced as a scannable image.
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

    // Customer cash orders no staff member has claimed yet.
    @GetMapping("/awaiting-pickup")
    public ApiResponse<PageResponse<OrderResponse>> listAwaitingPickup(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listAwaitingPickup(PageUtil.buildPageable(page, size))));
    }

    // Collects cash in person for a customer's cash order, pending or already being prepared.
    @PostMapping("/{id}/collect-cash")
    public ApiResponse<OrderResponse> collectCash(
            @PathVariable UUID id,
            @Valid @RequestBody CashPaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        OrderResponse response = orderService.collectCash(id, currentUser.getId(), request);
        return ApiResponse.of(HttpStatus.OK, "Cash collected successfully.", response);
    }

    // Customer orders with a Bakong QR generated, still unclaimed by staff.
    @GetMapping("/awaiting-bakong-confirmation")
    public ApiResponse<PageResponse<OrderResponse>> listAwaitingBakongConfirmation(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listAwaitingBakongConfirmation(PageUtil.buildPageable(page, size))));
    }

    // Confirms/accepts a customer's Bakong-paid order.
    @PostMapping("/{id}/accept-bakong")
    public ApiResponse<OrderResponse> acceptBakongPayment(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        OrderResponse response = orderService.acceptBakongPayment(id, currentUser.getId());
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, response);
    }

    // Unanswered "call staff" presses, oldest first — load once, then follow /topic/staff-calls.
    @GetMapping("/staff-calls")
    public ApiResponse<List<StaffCallResponse>> listStaffCalls() {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, staffCallService.listOpen());
    }

    // Takes the call: clears the alert on every staff screen and tells the customer.
    @PostMapping("/{id}/staff-call/answer")
    public ApiResponse<Void> answerStaffCall(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        staffCallService.answer(id, currentUser.getId());
        return ApiResponse.of(HttpStatus.OK, "Staff call answered.", null);
    }

    // Customer delivery orders waiting for a fee quote, oldest first. Each shows distanceMeters.
    @GetMapping("/awaiting-delivery-fee")
    public ApiResponse<PageResponse<OrderResponse>> listAwaitingDeliveryFee(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listAwaitingDeliveryFee(PageUtil.buildPageable(page, size))));
    }

    // Sets or revises the delivery fee for a pending delivery order.
    @PostMapping("/{id}/delivery-fee")
    public ApiResponse<OrderResponse> setDeliveryFee(
            @PathVariable UUID id, @Valid @RequestBody DeliveryFeeRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        OrderResponse response = orderService.setDeliveryFee(id, request.fee(), currentUser.getId());
        return ApiResponse.of(HttpStatus.OK, "Delivery fee set successfully.", response);
    }

    // The kitchen queue: PAID orders plus unpaid cash orders, which are fair game to start on.
    @GetMapping("/awaiting-preparation")
    public ApiResponse<PageResponse<OrderResponse>> listAwaitingPreparation(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listAwaitingPreparation(PageUtil.buildPageable(page, size))));
    }

    // Moves an order to PREPARING. A cash order can start here unpaid; cash is collected later.
    @PostMapping("/{id}/prepare")
    public ApiResponse<OrderResponse> startPreparing(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Order marked as preparing.",
                orderService.startPreparing(id, currentUser.getId()));
    }

    // Marks a pickup order as handed over. Rejects a delivery order or an unpaid cash order.
    @PostMapping("/{id}/complete")
    public ApiResponse<OrderResponse> completePickup(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Order completed successfully.",
                orderService.completePickup(id, currentUser.getId()));
    }

    // Marks a delivery order as out with a courier.
    @PostMapping("/{id}/dispatch")
    public ApiResponse<OrderResponse> dispatchForDelivery(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Order dispatched for delivery.",
                orderService.dispatchForDelivery(id, currentUser.getId()));
    }

    // Marks a delivery as arrived. Rejects an unpaid cash order — collect it first.
    @PostMapping("/{id}/deliver")
    public ApiResponse<OrderResponse> markDelivered(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Order marked as delivered.",
                orderService.markDelivered(id, currentUser.getId()));
    }

    // Everything currently out with a courier, oldest dispatch first.
    @GetMapping("/delivery-board")
    public ApiResponse<PageResponse<OrderResponse>> listDeliveryBoard(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listDeliveryBoard(PageUtil.buildPageable(page, size))));
    }
}
