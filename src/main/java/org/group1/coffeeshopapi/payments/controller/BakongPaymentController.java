package org.group1.coffeeshopapi.payments.controller;

import org.group1.coffeeshopapi.payments.dto.request.BakongQrRequest;
import org.group1.coffeeshopapi.payments.dto.request.CheckTransactionRequest;
import org.group1.coffeeshopapi.payments.dto.response.BakongResponse;
import org.group1.coffeeshopapi.payments.service.BakongPaymentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/payment", "/api/v1/payments/bakong"})
public class BakongPaymentController {

    private final BakongPaymentService bakongPaymentService;

    @PostMapping("/generate-qr")
    public KHQRResponse<KHQRData> generateQR(@Valid @RequestBody BakongQrRequest request) {
        return bakongPaymentService.generateQr(request);
    }

    @PostMapping(value = {"/qr-image", "/get-qr-image"}, produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQRImage(@RequestBody KHQRData qrData) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(bakongPaymentService.generateQrImage(qrData));
    }

    @PostMapping("/check-transaction")
    public BakongResponse checkTransaction(@Valid @RequestBody CheckTransactionRequest request) {
        return bakongPaymentService.checkTransaction(request);
    }
}
