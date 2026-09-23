package org.group1.coffeeshopapi.bakong.dto;

// Outcome of asking Bakong to turn a QR string into a payment app deeplink.
public record BakongDeeplinkResult(
        boolean success,
        String shortLink,
        String message,
        boolean failed
) {
    public static BakongDeeplinkResult success(String shortLink, String message) {
        return new BakongDeeplinkResult(true, shortLink, message, false);
    }

    /** The API answered, but declined to produce a deeplink for this QR. */
    public static BakongDeeplinkResult declined(String message) {
        return new BakongDeeplinkResult(false, null, message, false);
    }

    /** The API could not be asked, so no deeplink is available right now. */
    public static BakongDeeplinkResult failed(String message) {
        return new BakongDeeplinkResult(false, null, message, true);
    }
}
