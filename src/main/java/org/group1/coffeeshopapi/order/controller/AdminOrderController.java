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
import org.group1.coffeeshopapi.order.dto.response.StaffCallResponse;
import org.group1.coffeeshopapi.order.service.OrderService;
import org.group1.coffeeshopapi.order.service.ReceiptService;
import org.group1.coffeeshopapi.order.service.StaffCallService;
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
    private final StaffCallService staffCallService;
    // Also reachable by the Super Admin (hasRole("ADMIN") + role hierarchy).
    private final CurrentActor currentActor;

    // Rings up a walk-in sale in person, same as a barista can. Always pickup.
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

    // The printable receipt for any finished order (COMPLETED or DELIVERED).
    @GetMapping(value = "/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getReceipt(@PathVariable UUID id) {
        byte[] pdf = receiptService.generateReceiptPdf(id);
        return FileResponseUtil.respond(pdf, MediaType.APPLICATION_PDF, "receipt-" + id + ".pdf", true);
    }

    // Same document, but available as soon as the order is paid — no need to wait for it to be
    // prepared, delivered or completed first.
    @GetMapping(value = "/{id}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getInvoice(@PathVariable UUID id) {
        byte[] pdf = receiptService.generateInvoicePdf(id);
        return FileResponseUtil.respond(pdf, MediaType.APPLICATION_PDF, "invoice-" + id + ".pdf", true);
    }

    // Full audit trail for one order: created, paid, cancelled, etc.
    @GetMapping("/{id}/history")
    public ApiResponse<List<OrderAuditLogResponse>> getHistory(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, orderService.getHistory(id));
    }

    // Renders whatever Bakong QR is already stored on the order, for showing again at the register.
    @GetMapping(value = "/{id}/pay/bakong/qr/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getBakongQrImage(@PathVariable UUID id) {
        OrderResponse order = orderService.getAny(id);
        if (order.bakongQrString() == null) {
            throw new InvalidOperationException("No Bakong QR has been generated for this order yet");
        }
        byte[] png = QrImageUtil.toPng(order.bakongQrString(), QR_IMAGE_SIZE);
        return FileResponseUtil.respond(png, MediaType.IMAGE_PNG, "order-" + id + "-qr.png", true);
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
            @Valid @RequestBody CashPaymentRequest request) {
        OrderResponse response = orderService.collectCash(id, currentActor.id(), request);
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
    public ApiResponse<OrderResponse> acceptBakongPayment(@PathVariable UUID id) {
        OrderResponse response = orderService.acceptBakongPayment(id, currentActor.id());
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, response);
    }

    // Cancels any still-pending order, not just ones this admin rang up.
    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancel(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order cancelled successfully.",
                orderService.cancelAny(id, currentActor.id()));
    }

    // Unanswered "call staff" presses, oldest first — load once, then follow /topic/staff-calls.
    @GetMapping("/staff-calls")
    public ApiResponse<List<StaffCallResponse>> listStaffCalls() {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE, staffCallService.listOpen());
    }

    // Takes the call: clears the alert on every staff screen and tells the customer.
    @PostMapping("/{id}/staff-call/answer")
    public ApiResponse<Void> answerStaffCall(@PathVariable UUID id) {
        staffCallService.answer(id, currentActor.id());
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
            @PathVariable UUID id, @Valid @RequestBody DeliveryFeeRequest request) {
        OrderResponse response = orderService.setDeliveryFee(id, request.fee(), currentActor.id());
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
    public ApiResponse<OrderResponse> startPreparing(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order marked as preparing.",
                orderService.startPreparing(id, currentActor.id()));
    }

    // Marks a pickup order as handed over. Rejects a delivery order or an unpaid cash order.
    @PostMapping("/{id}/complete")
    public ApiResponse<OrderResponse> completePickup(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order completed successfully.",
                orderService.completePickup(id, currentActor.id()));
    }

    // Marks a delivery order as out with a courier.
    @PostMapping("/{id}/dispatch")
    public ApiResponse<OrderResponse> dispatchForDelivery(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order dispatched for delivery.",
                orderService.dispatchForDelivery(id, currentActor.id()));
    }

    // Marks a delivery as arrived. Rejects an unpaid cash order — collect it first.
    @PostMapping("/{id}/deliver")
    public ApiResponse<OrderResponse> markDelivered(@PathVariable UUID id) {
        return ApiResponse.of(HttpStatus.OK, "Order marked as delivered.",
                orderService.markDelivered(id, currentActor.id()));
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
