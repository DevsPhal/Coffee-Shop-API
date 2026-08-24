package org.group1.coffeeshopapi.bakong;

import org.group1.coffeeshopapi.bakong.dto.BakongTransactionCheckResult;

public interface BakongApiClient {
    BakongTransactionCheckResult checkTransactionByMd5(String md5Hash);
}
