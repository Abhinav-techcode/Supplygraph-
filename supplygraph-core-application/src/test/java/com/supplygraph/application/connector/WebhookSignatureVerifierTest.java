package com.supplygraph.application.connector;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureVerifierTest {
    private final WebhookSignatureVerifier v = new WebhookSignatureVerifier();
    private final byte[] secret = "s3cret".getBytes(StandardCharsets.UTF_8);
    private final byte[] body = "[{\"id\":\"1\"}]".getBytes(StandardCharsets.UTF_8);
    private final Instant now = Instant.parse("2026-11-18T10:00:00Z");

    @Test void acceptsValidSignatureWithinWindow() {
        long ts = now.getEpochSecond();
        assertThat(v.verify(secret, "" + ts, WebhookSignatureVerifier.sign(secret, ts, body), body, now)).isTrue();
    }

    @Test void rejectsTamperedBodyWrongSecretAndMalformedHeaders() {
        long ts = now.getEpochSecond();
        String sig = WebhookSignatureVerifier.sign(secret, ts, body);
        assertThat(v.verify(secret, "" + ts, sig, "[]".getBytes(), now)).isFalse();
        assertThat(v.verify("other".getBytes(), "" + ts, sig, body, now)).isFalse();
        assertThat(v.verify(secret, "abc", sig, body, now)).isFalse();
        assertThat(v.verify(secret, null, sig, body, now)).isFalse();
        assertThat(v.verify(secret, "" + ts, null, body, now)).isFalse();
        assertThat(v.verify(new byte[0], "" + ts, sig, body, now)).isFalse();
    }

    @Test void rejectsStaleAndFutureTimestamps() {
        long old = now.minusSeconds(600).getEpochSecond();
        assertThat(v.verify(secret, "" + old, WebhookSignatureVerifier.sign(secret, old, body), body, now)).isFalse();
        long future = now.plusSeconds(600).getEpochSecond();
        assertThat(v.verify(secret, "" + future, WebhookSignatureVerifier.sign(secret, future, body), body, now)).isFalse();
    }
}
