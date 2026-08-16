package org.group1.coffeeshopapi.payments.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

public record BakongTransactionData(
        String hash,
        String fromAccountId,
        String toAccountId,
        String currency,
        Double amount,
        String description,
        @JsonAlias("createDateMs")
        Long createdDateMs,
        @JsonAlias("acknowledgeDateMs")
        Long acknowledgedDateMs,
        String externalRef
) {
}
