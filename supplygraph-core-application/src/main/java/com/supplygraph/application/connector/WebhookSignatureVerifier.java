package com.supplygraph.application.connector;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

/** HMAC-SHA256 over "{timestamp}.{body}", constant-time comparison, bounded timestamp window (replay protection with the inbox). */
public class WebhookSignatureVerifier {
    public static final Duration MAX_SKEW = Duration.ofMinutes(5);

    public static String sign(byte[] secret, long epochSeconds, byte[] body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            mac.update((epochSeconds + ".").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }

    public boolean verify(byte[] secret, String timestampHeader, String signatureHeader, byte[] body, Instant now) {
        if (secret == null || secret.length == 0 || timestampHeader == null || signatureHeader == null) return false;
        long ts;
        try { ts = Long.parseLong(timestampHeader.trim()); } catch (NumberFormatException e) { return false; }
        if (Duration.between(Instant.ofEpochSecond(ts), now).abs().compareTo(MAX_SKEW) > 0) return false;
        byte[] expected = sign(secret, ts, body).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signatureHeader.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
