package org.group1.coffeeshopapi.telegram.util;

import org.group1.coffeeshopapi.auth.dto.request.TelegramWidgetAuthRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

// Verifies a Telegram Login Widget payload really came from Telegram, unmodified, using the bot
// token as a shared secret.
public final class TelegramWidgetAuthVerifier {

    // Rejects a payload once it's too old, so a captured response can't be replayed indefinitely.
    private static final long MAX_AUTH_AGE_SECONDS = 86_400;

    private TelegramWidgetAuthVerifier() {
    }

    public static boolean isFresh(TelegramWidgetAuthRequest request) {
        long ageSeconds = Instant.now().getEpochSecond() - request.authDate();
        return ageSeconds >= 0 && ageSeconds <= MAX_AUTH_AGE_SECONDS;
    }

    public static boolean isValidSignature(TelegramWidgetAuthRequest request, String botToken) {
        byte[] expected = hmacSha256(buildDataCheckString(request), sha256(botToken.getBytes(StandardCharsets.UTF_8)));
        byte[] actual;
        try {
            actual = HexFormat.of().parseHex(request.hash());
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return MessageDigest.isEqual(expected, actual);
    }

    // "key=value" lines, one per field Telegram included, sorted and joined with \n.
    private static String buildDataCheckString(TelegramWidgetAuthRequest request) {
        List<String> fields = new ArrayList<>();
        fields.add("auth_date=" + request.authDate());
        fields.add("id=" + request.id());
        if (request.firstName() != null) {
            fields.add("first_name=" + request.firstName());
        }
        if (request.lastName() != null) {
            fields.add("last_name=" + request.lastName());
        }
        if (request.photoUrl() != null) {
            fields.add("photo_url=" + request.photoUrl());
        }
        if (request.username() != null) {
            fields.add("username=" + request.username());
        }
        Collections.sort(fields);
        return String.join("\n", fields);
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static byte[] hmacSha256(String data, byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HmacSHA256 not available", ex);
        }
    }
}
