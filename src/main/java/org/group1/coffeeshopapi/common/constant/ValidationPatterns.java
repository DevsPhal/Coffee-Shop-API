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

    // @Pattern skips null, so on a PATCH this rejects "" or spaces while null still means "leave it unchanged".
    public static final String NOT_BLANK_REGEX = "(?s).*\\S.*";

    // Money columns are numeric(12,2): at most 10 digits before the decimal point.
    public static final int MONEY_INTEGER_DIGITS = 10;

    // Upper bound for one cart or order line, so quantities can't overflow when lines are combined.
    public static final int MAX_ITEM_QUANTITY = 999;

    // BCrypt only reads the first 72 bytes of a password.
    public static final int PASSWORD_MAX_LENGTH = 72;
}
