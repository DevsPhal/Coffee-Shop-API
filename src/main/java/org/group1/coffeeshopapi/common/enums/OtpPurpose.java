package org.group1.coffeeshopapi.common.enums;

public enum OtpPurpose {
    REGISTER("Account Verification"),
    LOGIN("Login Verification"),
    RESET_PASSWORD("Password Reset");

    private final String label;

    OtpPurpose(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
