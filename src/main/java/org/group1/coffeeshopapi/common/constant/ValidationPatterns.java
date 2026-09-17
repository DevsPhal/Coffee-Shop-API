package org.group1.coffeeshopapi.common.constant;

// Shared regex patterns for validated request fields, so every DTO uses the same rules.
public final class ValidationPatterns {
    private ValidationPatterns() {}

    // Cambodian phone number, e.g. "072 345 5674" or "0723455674". Also accepts an empty string,
    // since some fields use "" to mean "clear the phone number" and null to mean "leave unchanged".
    public static final String CAMBODIA_PHONE_REGEX = "^$|^0\\d{2}\\s?\\d{3}\\s?\\d{3,4}$";
    public static final String CAMBODIA_PHONE_MESSAGE =
            "Phone number must be a valid Cambodian number (9 to 10 digits), e.g. 072 345 5674";

    // At least 8 characters with an uppercase letter, a lowercase letter, a digit, and a special character.
    public static final String STRONG_PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$";
    public static final String STRONG_PASSWORD_MESSAGE =
            "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character";
}
