package org.group1.coffeeshopapi.payments.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import org.group1.coffeeshopapi.common.exception.InvalidPaymentException;
import org.group1.coffeeshopapi.payments.dto.request.BakongQrRequest;
import org.group1.coffeeshopapi.payments.dto.request.CheckTransactionRequest;
import org.group1.coffeeshopapi.payments.dto.response.BakongQrResult;
import org.group1.coffeeshopapi.payments.dto.response.BakongResponse;
import org.group1.coffeeshopapi.payments.dto.response.BakongTransactionCheckResult;
import org.group1.coffeeshopapi.payments.service.BakongPaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import kh.gov.nbc.bakong_khqr.BakongKHQR;
import kh.gov.nbc.bakong_khqr.model.KHQRData;
import kh.gov.nbc.bakong_khqr.model.KHQRCurrency;
import kh.gov.nbc.bakong_khqr.model.KHQRResponse;
import kh.gov.nbc.bakong_khqr.model.MerchantInfo;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BakongPaymentServiceImpl implements BakongPaymentService {

    private static final int QR_IMAGE_SIZE = 300;
    private static final Duration TOKEN_EXPIRATION_SKEW = Duration.ofMinutes(1);
    private static final String RENEW_TOKEN_PATH = "/v1/renew_token";
    private static final String CHECK_TRANSACTION_PATH = "/v1/check_transaction_by_md5";

    private final RestTemplate bakongRestTemplate;
    private final ObjectMapper objectMapper;

    @Value("${bakong.email:}")
    private String email;

    @Value("${bakong.token:}")
    private String configuredToken;

    @Value("${bakong.account-id}")
    private String bakongAccountId;

    @Value("${bakong.merchant-name}")
    private String merchantName;

    @Value("${bakong.merchant-city}")
    private String merchantCity;

    @Value("${bakong.merchant-category-code:5999}")
    private String merchantCategoryCode;

    @Value("${bakong.merchant-id:}")
    private String merchantId;

    @Value("${bakong.acquiring-bank:}")
    private String acquiringBank;

    @Value("${bakong.store-label:590st Cafe}")
    private String storeLabel;

    @Value("${bakong.terminal-label:POS}")
    private String terminalLabel;

    @Value("${bakong.mobile-number:}")
    private String mobileNumber;

    @Value("${bakong.purpose-of-transaction:Payment}")
    private String purposeOfTransaction;

    @Value("${bakong.expiration-minutes:15}")
    private long expirationMinutes;

    private String cachedToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    @Override
    public BakongQrResult generateForPayment(BigDecimal amount, String currency, String billNumber) {
        BakongQrRequest request = new BakongQrRequest();
        request.setAmount(amount.doubleValue());
        request.setCurrency(toCurrency(currency));
        request.setBillNumber(billNumber);

        KHQRResponse<KHQRData> response = generateQr(request);
        if (response.getKHQRStatus() == null || response.getKHQRStatus().getCode() != 0 || response.getData() == null) {
            String message = response.getKHQRStatus() == null
                    ? "Unknown KHQR generation error"
                    : response.getKHQRStatus().getMessage();
            throw new InvalidPaymentException(message);
        }

        return new BakongQrResult(response.getData().getQr(), response.getData().getMd5());
    }

    @Override
    public KHQRResponse<KHQRData> generateQr(BakongQrRequest request) {
        validateAmount(request.getAmount());
        return BakongKHQR.generateMerchant(toMerchantInfo(request));
    }

    @Override
    public byte[] generateQrImage(KHQRData qrData) {
        if (qrData == null || qrData.getQr() == null || qrData.getQr().isBlank()) {
            throw new InvalidPaymentException("KHQR qr value is required");
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            BitMatrix bitMatrix = new QRCodeWriter().encode(
                    qrData.getQr(),
                    BarcodeFormat.QR_CODE,
                    QR_IMAGE_SIZE,
                    QR_IMAGE_SIZE
            );
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new InvalidPaymentException("Unable to generate KHQR image: " + e.getMessage());
        }
    }

    @Override
    public BakongResponse checkTransaction(CheckTransactionRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getToken());

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("md5", request.md5()), headers);

        try {
            BakongResponse response = bakongRestTemplate.postForObject(
                    CHECK_TRANSACTION_PATH, entity, BakongResponse.class);
            return response == null
                    ? new BakongResponse(1, "Empty response from Bakong", null, null)
                    : response;
        } catch (RestClientException e) {
            return new BakongResponse(1, "Not yet paid or Bakong API unavailable: " + e.getMessage(), null, null);
        }
    }

    @Override
    public BakongTransactionCheckResult checkTransactionByMd5(String md5Hash) {
        BakongResponse response = checkTransaction(new CheckTransactionRequest(md5Hash));
        boolean paid = response.responseCode() == 0;
        String transactionHash = paid && response.data() != null ? response.data().hash() : null;
        return new BakongTransactionCheckResult(paid, transactionHash, response.responseMessage());
    }

    private MerchantInfo toMerchantInfo(BakongQrRequest request) {
        MerchantInfo merchantInfo = new MerchantInfo();
        merchantInfo.setBakongAccountId(required(valueOrDefault(request.getBakongAccountId(), bakongAccountId), "Bakong account ID"));
        merchantInfo.setMerchantName(required(valueOrDefault(request.getMerchantName(), merchantName), "Bakong merchant name"));
        merchantInfo.setMerchantCity(required(valueOrDefault(request.getMerchantCity(), merchantCity), "Bakong merchant city"));
        merchantInfo.setMerchantCategoryCode(valueOrDefault(request.getMerchantCategoryCode(), merchantCategoryCode));
        merchantInfo.setMerchantId(valueOrDefault(request.getMerchantId(), merchantId));
        merchantInfo.setAcquiringBank(valueOrDefault(request.getAcquiringBank(), acquiringBank));
        merchantInfo.setCurrency(request.getCurrency() == null ? KHQRCurrency.USD : request.getCurrency());
        merchantInfo.setAmount(request.getAmount());
        merchantInfo.setBillNumber(valueOrDefault(request.getBillNumber(), "BILL" + System.currentTimeMillis()));
        merchantInfo.setStoreLabel(valueOrDefault(request.getStoreLabel(), storeLabel));
        merchantInfo.setTerminalLabel(valueOrDefault(request.getTerminalLabel(), terminalLabel));
        merchantInfo.setMobileNumber(valueOrDefault(request.getMobileNumber(), mobileNumber));
        merchantInfo.setPurposeOfTransaction(valueOrDefault(request.getPurposeOfTransaction(), purposeOfTransaction));
        merchantInfo.setUpiAccountInformation(request.getUpiAccountInformation());
        merchantInfo.setMerchantAlternateLanguagePreference(request.getMerchantAlternateLanguagePreference());
        merchantInfo.setMerchantNameAlternateLanguage(request.getMerchantNameAlternateLanguage());
        merchantInfo.setMerchantCityAlternateLanguage(request.getMerchantCityAlternateLanguage());
        merchantInfo.setExpirationTimestamp(resolveExpirationTimestamp(request.getExpirationTimestamp()));
        return merchantInfo;
    }

    private synchronized String getToken() {
        if (configuredToken != null && !configuredToken.isBlank()) {
            return configuredToken;
        }
        if (cachedToken != null && Instant.now().plus(TOKEN_EXPIRATION_SKEW).isBefore(tokenExpiresAt)) {
            return cachedToken;
        }
        if (email == null || email.isBlank()) {
            throw new InvalidPaymentException("Bakong API email is not configured (set BAKONG_EMAIL or bakong.email)");
        }

        try {
            RenewTokenResponse response = bakongRestTemplate.postForObject(
                    RENEW_TOKEN_PATH,
                    Map.of("email", email),
                    RenewTokenResponse.class
            );
            if (response == null || response.responseCode() != 0 || response.data() == null
                    || response.data().token() == null || response.data().token().isBlank()) {
                String message = response == null ? "Empty response from Bakong token renewal" : response.responseMessage();
                throw new InvalidPaymentException(message);
            }

            cachedToken = response.data().token();
            tokenExpiresAt = decodeExpiration(cachedToken);
            return cachedToken;
        } catch (RestClientException e) {
            throw new InvalidPaymentException("Unable to renew Bakong API token: " + e.getMessage());
        }
    }

    private Instant decodeExpiration(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return Instant.now().plus(Duration.ofHours(1));
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JwtPayload payload = objectMapper.readValue(payloadJson, JwtPayload.class);
            return payload.exp() == null ? Instant.now().plus(Duration.ofHours(1)) : Instant.ofEpochSecond(payload.exp());
        } catch (Exception e) {
            return Instant.now().plus(Duration.ofHours(1));
        }
    }

    private KHQRCurrency toCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return KHQRCurrency.USD;
        }
        try {
            return KHQRCurrency.valueOf(currency.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidPaymentException("Unsupported currency for Bakong KHQR: " + currency);
        }
    }

    private Long resolveExpirationTimestamp(Long requestedExpirationTimestamp) {
        if (requestedExpirationTimestamp != null) {
            return requestedExpirationTimestamp;
        }
        return System.currentTimeMillis() + Duration.ofMinutes(expirationMinutes).toMillis();
    }

    private void validateAmount(Double amount) {
        if (amount == null || amount <= 0) {
            throw new InvalidPaymentException("Bakong KHQR amount must be greater than zero");
        }
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new InvalidPaymentException(label + " is not configured");
        }
        return value;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RenewTokenResponse(int responseCode, String responseMessage, Integer errorCode, TokenData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenData(String token) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JwtPayload(Long exp) {
    }
}
