package org.group1.coffeeshopapi.common.constant;

public class RedisKeys {
    private RedisKeys(){}

    public static final String OTP_PREFIX = "otp:";
    public static final String OTP_ATTEMPTS_SUFFIX = ":attempts";
    public static final String OTP_COOLDOWN_SUFFIX = ":cooldown";

    public static final String LOGIN_TICKET_PREFIX = "authticket:";
    public static final String REFRESH_TOKEN_PREFIX = "refresh:";
    public static final String JWT_DENYLIST_PREFIX = "jwt:denylist:";
    public static final String TELEGRAM_LINK_CODE_PREFIX = "tg:link:";

    public static String otpKey(String purpose, String email) {
        return OTP_PREFIX + purpose.toLowerCase() + ":" + email.toLowerCase();
    }

    public static String otpAttemptsKey(String purpose, String email) {
        return otpKey(purpose, email) + OTP_ATTEMPTS_SUFFIX;
    }

    public static String otpCooldownKey(String purpose, String email) {
        return otpKey(purpose, email) + OTP_COOLDOWN_SUFFIX;
    }
}
