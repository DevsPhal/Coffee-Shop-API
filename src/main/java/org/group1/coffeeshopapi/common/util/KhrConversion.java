package org.group1.coffeeshopapi.common.util;

import org.group1.coffeeshopapi.common.exception.InvalidOperationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * USD to riel for anything shown or collected in KHR: Bakong KHQR amounts, cash due and change, receipts.
 *
 * <p>Riel amounts are rounded to the nearest 100: last two digits 00-50 round down, 51-99 round up
 * (1,250 → 1,200, 1,251 → 1,300). The USD amount and the exchange rate are never rounded; only the final
 * riel figure is. Converting first makes a whole number of riel (half up), so the rule always sees the
 * two digits a person reads on the amount.
 */
public final class KhrConversion {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal FIFTY = BigDecimal.valueOf(50);

    private KhrConversion() {
    }

    /** The riel amount for {@code usd} at {@code khrPerUsd}, rounded to the nearest 100 riel. */
    public static BigDecimal fromUsd(BigDecimal usd, BigDecimal khrPerUsd) {
        requireRate(khrPerUsd);
        return roundToNearest100(usd.multiply(khrPerUsd));
    }

    /** A riel amount rounded to the nearest 100: 00-50 down, 51-99 up. */
    public static BigDecimal roundToNearest100(BigDecimal khr) {
        BigDecimal whole = khr.setScale(0, RoundingMode.HALF_UP);
        if (whole.signum() < 0) {
            return roundToNearest100(whole.negate()).negate();
        }
        BigDecimal remainder = whole.remainder(HUNDRED);
        BigDecimal down = whole.subtract(remainder);
        return remainder.compareTo(FIFTY) <= 0 ? down : down.add(HUNDRED);
    }

    /** A riel amount in USD (cents, half up), e.g. riel change handed back in dollars. */
    public static BigDecimal toUsd(BigDecimal khr, BigDecimal khrPerUsd) {
        requireRate(khrPerUsd);
        return khr.divide(khrPerUsd, 2, RoundingMode.HALF_UP);
    }

    public static void requireRate(BigDecimal khrPerUsd) {
        if (khrPerUsd == null || khrPerUsd.signum() <= 0) {
            throw new InvalidOperationException("USD-to-KHR exchange rate is not configured");
        }
    }
}
