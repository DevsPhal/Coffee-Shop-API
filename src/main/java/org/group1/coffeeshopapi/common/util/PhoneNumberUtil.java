package org.group1.coffeeshopapi.common.util;

// ValidationPatterns.CAMBODIA_PHONE_REGEX allows optional spaces (e.g. "072 345 5674" or
// "0723455674" are both valid), so the same real number can arrive in different textual forms.
// Stripping whitespace before it's ever persisted keeps one canonical form on file, so the
// uniqueness constraint/check on phone_number actually catches duplicates instead of two
// differently-formatted submissions of the same number silently coexisting.
public final class PhoneNumberUtil {
    private PhoneNumberUtil() {}

    public static String normalize(String phoneNumber) {
        return phoneNumber == null ? null : phoneNumber.replaceAll("\\s+", "");
    }

    // Canonicalizes to bare digits with the 855 country code, so a locally-entered number
    // ("072 345 5674") and the E.164-ish digits Telegram hands back from a shared contact
    // ("85572345674", no leading +) compare equal regardless of which form either one arrived in.
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

    // Used to verify a Telegram invite: does the phone number an admin typed in at invite time
    // match the one the invitee actually shared from their own Telegram contact card?
    public static boolean matches(String a, String b) {
        String canonicalA = toE164Cambodia(a);
        String canonicalB = toE164Cambodia(b);
        return canonicalA != null && !canonicalA.isEmpty() && canonicalA.equals(canonicalB);
    }
}
