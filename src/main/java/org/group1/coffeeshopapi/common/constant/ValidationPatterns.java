package org.group1.coffeeshopapi.common.constant;

public final class ValidationPatterns {
    private ValidationPatterns() {}

    public static final String CAMBODIA_PHONE_REGEX = "^$|^0\\d{2}\\s?\\d{3}\\s?\\d{3,4}$";
    public static final String CAMBODIA_PHONE_MESSAGE =
            "Phone number must be a valid Cambodian number (9 to 10 digits), e.g. 072 345 5674";

    public static final String STRONG_PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$";
    public static final String STRONG_PASSWORD_MESSAGE =
            "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character";

    public static final String SKU_REGEX = "^$|^[A-Za-z0-9]+([-_.][A-Za-z0-9]+)*$";
    public static final String SKU_MESSAGE =
            "SKU may only contain letters, digits and single '-', '_' or '.' separators, e.g. FD-COF-IL-001";
    public static final int SKU_MAX_LENGTH = 64;
}
