package org.group1.coffeeshopapi.payments.dto.response;

import java.time.LocalDateTime;

import org.group1.coffeeshopapi.common.enums.PaymentMethod;
import org.group1.coffeeshopapi.common.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private PaymentMethod method;
    private PaymentStatus status;
    private double amount;
    private String currency;
    private boolean verified;
    private String qrString;
    private String bakongTransactionHash;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
