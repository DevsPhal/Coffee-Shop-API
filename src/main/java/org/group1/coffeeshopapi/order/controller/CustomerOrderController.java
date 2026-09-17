package org.group1.coffeeshopapi.order.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
@Tag(name = "Customer Orders", description = "Customer only: pay for and track own orders")
@SecurityRequirement(name = "bearerAuth")
public class CustomerOrderController {

    // A 300x300 PNG scans reliably on a phone camera without being needlessly large to transfer.
    private static final int QR_IMAGE_SIZE = 300;

    private final OrderService orderService;
    private final ReceiptService receiptService;

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                PageResponse.of(orderService.listOwnForCustomer(currentUser.getId(), status, PageUtil.buildPageable(page, size))));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getById(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                orderService.getOwnForCustomer(id, currentUser.getId()));
    }

    // The receipt for a finished order (COMPLETED or DELIVERED).
    @GetMapping(value = "/{id}/receipt", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getReceipt(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        orderService.getOwnForCustomer(id, currentUser.getId());
        byte[] pdf = receiptService.generateReceiptPdf(id);
        return FileResponseUtil.respond(pdf, MediaType.APPLICATION_PDF, "receipt-" + id + ".pdf", true);
    }

    // Same document, but available as soon as the order is paid — no need to wait for pickup or
    // delivery.
    @GetMapping(value = "/{id}/invoice", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getInvoice(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        orderService.getOwnForCustomer(id, currentUser.getId());
        byte[] pdf = receiptService.generateInvoicePdf(id);
        return FileResponseUtil.respond(pdf, MediaType.APPLICATION_PDF, "invoice-" + id + ".pdf", true);
    }

    // Despite the name, this applies to delivery orders too.
    @PostMapping("/{id}/pay/cash-on-pickup")
    public ApiResponse<OrderResponse> payCashOnPickup(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Order will be paid with cash.",
                orderService.selectCashOnPickup(id, currentUser.getId()));
    }

    @PostMapping("/{id}/pay/bakong/qr")
    public ApiResponse<BakongQrResponse> generateBakongQr(
            @PathVariable UUID id,
            @RequestParam(required = false) Currency currency,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, "Bakong KHQR generated successfully.",
                orderService.generateBakongQrForCustomer(id, currentUser.getId(), currency));
    }

    @PostMapping("/{id}/pay/bakong/confirm")
    public ApiResponse<OrderResponse> confirmBakongPayment(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ApiResponse.of(HttpStatus.OK, AppConstant.SUCCESS_MESSAGE,
                orderService.confirmBakongPaymentForCustomer(id, currentUser.getId()));
    }

    // Renders the QR string generateBakongQr already produced as a scannable image.
    @GetMapping(value = "/{id}/pay/bakong/qr/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getBakongQrImage(
            @PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails currentUser) {
        OrderResponse order = orderService.getOwnForCustomer(id, currentUser.getId());
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
                orderService.cancelForCustomer(id, currentUser.getId()));
    }
}
