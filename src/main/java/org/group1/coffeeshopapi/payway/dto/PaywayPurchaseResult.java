package org.group1.coffeeshopapi.payway.dto;

// Outcome of asking PayWay for an ABA Mobile deeplink.
public record PaywayPurchaseResult(
        boolean success,
        String deeplink,
        String message
) {
    public static PaywayPurchaseResult success(String deeplink) {
        return new PaywayPurchaseResult(true, deeplink, null);
    }

    public static PaywayPurchaseResult failed(String message) {
        return new PaywayPurchaseResult(false, null, message);
    }
}
