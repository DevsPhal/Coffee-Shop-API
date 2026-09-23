package org.group1.coffeeshopapi.bakong;

import org.group1.coffeeshopapi.bakong.dto.BakongDeeplinkResult;
import org.group1.coffeeshopapi.bakong.dto.BakongTransactionCheckResult;

public interface BakongApiClient {
    BakongTransactionCheckResult checkTransactionByMd5(String md5Hash);

    // Turns a KHQR string into a link that opens the customer's banking app directly.
    BakongDeeplinkResult generateDeeplink(String qrString);
}
