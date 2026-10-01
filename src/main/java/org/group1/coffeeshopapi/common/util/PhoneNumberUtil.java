package org.group1.coffeeshopapi.common.util;

public final class PhoneNumberUtil {
    private PhoneNumberUtil() {}

    public static String normalize(String phoneNumber) {
        return phoneNumber == null ? null : phoneNumber.replaceAll("\\s+", "");
    }

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

    public static boolean matches(String a, String b) {
        String canonicalA = toE164Cambodia(a);
        String canonicalB = toE164Cambodia(b);
        return canonicalA != null && !canonicalA.isEmpty() && canonicalA.equals(canonicalB);
    }
}
