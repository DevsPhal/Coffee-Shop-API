package org.group1.coffeeshopapi.auth.dto.response;

// Most accounts get otpRequired=true and must verify next; the super admin skips OTP and gets
// tokens directly.
public record LoginResponse(boolean otpRequired, String loginTicket, AuthTokenResponse tokens) {

    public static LoginResponse otpChallenge(String loginTicket) {
        return new LoginResponse(true, loginTicket, null);
    }

    public static LoginResponse authenticated(AuthTokenResponse tokens) {
        return new LoginResponse(false, null, tokens);
    }
}