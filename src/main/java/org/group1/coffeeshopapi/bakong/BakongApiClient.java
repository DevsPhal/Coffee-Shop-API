package org.group1.coffeeshopapi.bakong;

import org.group1.coffeeshopapi.bakong.dto.BakongDeeplinkResult;
import org.group1.coffeeshopapi.bakong.dto.BakongTransactionCheckResult;

public interface BakongApiClient {
    /** Checks on the shared, rate-limited cadence; may answer with the last known result instead of calling Bakong. */
    BakongTransactionCheckResult checkTransactionByMd5(String md5Hash);

    /**
     * Same as {@link #checkTransactionByMd5(String)}; {@code promptly} is for a person asking right now
     * ("I've paid", a barista's Accept), which is allowed to re-check sooner.
     */
    BakongTransactionCheckResult checkTransactionByMd5(String md5Hash, boolean promptly);

    BakongDeeplinkResult generateDeeplink(String qrString, String callbackUrl);
}
