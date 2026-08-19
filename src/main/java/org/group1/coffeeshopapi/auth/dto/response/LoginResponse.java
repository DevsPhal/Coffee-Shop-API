package org.group1.coffeeshopapi.auth.dto.response;

/**
 * Unified login result. Most accounts get {@code otpRequired=true} and must call
 * verify-login-otp next; the super admin account skips OTP entirely and gets {@code tokens}
 * directly from /login. The user-facing message lives in the enclosing ApiResponse envelope,
 * chosen by the controller based on {@code otpRequired}.
 */
public record LoginResponse(boolean otpRequired, String loginTicket, AuthTokenResponse tokens) {

    public static LoginResponse otpChallenge(String loginTicket) {
        return new LoginResponse(true, loginTicket, null);
    }

    public static LoginResponse authenticated(AuthTokenResponse tokens) {
        return new LoginResponse(false, null, tokens);
    }
}