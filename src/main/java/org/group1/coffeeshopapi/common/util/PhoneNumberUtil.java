package org.group1.coffeeshopapi.common.util;

// A phone number can be typed with or without spaces (e.g. "072 345 5674" vs "0723455674").
// These helpers normalize both forms so duplicate checks and comparisons actually match.
public final class PhoneNumberUtil {
    private PhoneNumberUtil() {}

    public static String normalize(String phoneNumber) {
        return phoneNumber == null ? null : phoneNumber.replaceAll("\\s+", "");
    }

    // Converts to bare digits with the 855 country code, so a locally-typed number and the
    // digits Telegram shares from a contact card compare equal.
    public static String toE164Cambodia(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        String digits = phoneNumber.replaceAll("\\D", "");
        if (digits.startsWith("855")) {
            return digits;
        }
        if (digits.startsWith("0")) {
            return "855" + digits.substring(1);
        }
        return digits;
    }

    // True if two phone numbers are the same, ignoring formatting.
    public static boolean matches(String a, String b) {
        String canonicalA = toE164Cambodia(a);
        String canonicalB = toE164Cambodia(b);
        return canonicalA != null && !canonicalA.isEmpty() && canonicalA.equals(canonicalB);
    }
}
