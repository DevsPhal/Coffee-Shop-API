package org.group1.coffeeshopapi.payments.service;

import java.math.BigDecimal;

import org.group1.coffeeshopapi.payments.dto.request.BakongQrRequest;
import org.group1.coffeeshopapi.payments.dto.request.CheckTransactionRequest;
import org.group1.coffeeshopapi.payments.dto.response.BakongQrResult;
import org.group1.coffeeshopapi.payments.dto.response.BakongResponse;
import org.group1.coffeeshopapi.payments.dto.response.BakongTransactionCheckResult;

import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;

public interface BakongPaymentService {

    BakongQrResult generateForPayment(BigDecimal amount, String currency, String billNumber);

    KHQRResponse<KHQRData> generateQr(BakongQrRequest request);

    byte[] generateQrImage(KHQRData qrData);

    BakongResponse checkTransaction(CheckTransactionRequest request);

    BakongTransactionCheckResult checkTransactionByMd5(String md5Hash);
}
