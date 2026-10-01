package org.group1.coffeeshopapi.bakong.dto;

public record BakongDeeplinkResult(
        boolean success,
        String shortLink,
        String message,
        boolean failed
) {
    public static BakongDeeplinkResult success(String shortLink, String message) {
        return new BakongDeeplinkResult(true, shortLink, message, false);
    }

    public static BakongDeeplinkResult declined(String message) {
        return new BakongDeeplinkResult(false, null, message, false);
    }

    public static BakongDeeplinkResult failed(String message) {
        return new BakongDeeplinkResult(false, null, message, true);
    }
}
