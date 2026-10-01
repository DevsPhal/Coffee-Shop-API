package org.group1.coffeeshopapi.auth.dto.response;

public record LoginResponse(boolean otpRequired, String loginTicket, AuthTokenResponse tokens) {

    public static LoginResponse otpChallenge(String loginTicket) {
        return new LoginResponse(true, loginTicket, null);
    }

    public static LoginResponse authenticated(AuthTokenResponse tokens) {
        return new LoginResponse(false, null, tokens);
    }
}
