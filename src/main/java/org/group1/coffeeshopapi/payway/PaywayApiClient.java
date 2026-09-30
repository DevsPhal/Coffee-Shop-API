package org.group1.coffeeshopapi.payway;

import org.group1.coffeeshopapi.payway.dto.PaywayPurchaseResult;
import org.group1.coffeeshopapi.payway.dto.PaywayTransactionCheckResult;

import java.math.BigDecimal;

public interface PaywayApiClient {

    // Opens a USD payment in PayWay and returns the ABA Mobile deeplink for it.
    PaywayPurchaseResult createAbaDeeplink(String tranId, BigDecimal amountUsd);

    PaywayTransactionCheckResult checkTransaction(String tranId);
}
