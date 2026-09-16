package org.group1.coffeeshopapi.common.constant;

/**
 * Shared regex patterns (and their matching messages) for {@code @Pattern}-validated
 * request fields, so every DTO enforces the same rules instead of redefining its own.
 */
public final class ValidationPatterns {
    private ValidationPatterns() {}

    // Cambodian phone numbers: leading 0, then 8-9 more digits (9-10 digits total),
    // optionally grouped as shown on the number, e.g. "072 345 5674" or "0723455674". The empty
    // string is deliberately accepted too: every field this backs is optional, and callers
    // (UserProfileServiceImpl#updateProfile, StaffServiceImpl#update, ...) use "" to mean "clear
    // the phone number" versus null meaning "leave it unchanged" — @Pattern only skips an actual
    // null, so without this alternation that clear-the-field convention fails validation outright.
    public static final String CAMBODIA_PHONE_REGEX = "^$|^0\\d{2}\\s?\\d{3}\\s?\\d{3,4}$";
    public static final String CAMBODIA_PHONE_MESSAGE =
            "Phone number must be a valid Cambodian number (9 to 10 digits), e.g. 072 345 5674";

    // At least 8 characters with an uppercase letter, a lowercase letter, a digit, and a special character.
    public static final String STRONG_PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$";
    public static final String STRONG_PASSWORD_MESSAGE =
            "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character";
}
