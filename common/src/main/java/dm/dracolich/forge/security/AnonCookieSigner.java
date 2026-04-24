package dm.dracolich.forge.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

public class AnonCookieSigner {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String SEPARATOR = ".";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final byte[] secret;

    public AnonCookieSigner(String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("Cookie secret must be at least 32 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(String anonId, Instant expiresAt) {
        String payload = anonId + SEPARATOR + expiresAt.toEpochMilli();
        String hmac = hmac(payload);
        return payload + SEPARATOR + hmac;
    }

    public Optional<String> parse(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) {
            return Optional.empty();
        }
        String[] parts = cookieValue.split("\\.", 3);
        if (parts.length != 3) {
            return Optional.empty();
        }
        String anonId = parts[0];
        String expiresAtRaw = parts[1];
        String providedHmac = parts[2];

        long expiresAtMs;
        try {
            expiresAtMs = Long.parseLong(expiresAtRaw);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        if (Instant.ofEpochMilli(expiresAtMs).isBefore(Instant.now())) {
            return Optional.empty();
        }

        String expectedHmac = hmac(anonId + SEPARATOR + expiresAtRaw);
        if (!constantTimeEquals(expectedHmac, providedHmac)) {
            return Optional.empty();
        }
        return Optional.of(anonId);
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret, HMAC_ALGO));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return ENCODER.encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }
}
