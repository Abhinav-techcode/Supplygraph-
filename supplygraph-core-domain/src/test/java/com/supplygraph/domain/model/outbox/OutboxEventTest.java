package com.supplygraph.domain.model.outbox;

import com.supplygraph.domain.model.WorkspaceId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class OutboxEventTest {
    private final BackoffPolicy policy = new BackoffPolicy(Duration.ofSeconds(2), Duration.ofSeconds(10), 3);
    private OutboxEvent ev() { return OutboxEvent.create(WorkspaceId.of("ws"), "T", "agg", "{}", "key", null); }

    @Test void backoffIsExponentialAndCapped() {
        assertThat(policy.delayForAttempt(1)).isEqualTo(Duration.ofSeconds(2));
        assertThat(policy.delayForAttempt(2)).isEqualTo(Duration.ofSeconds(4));
        assertThat(policy.delayForAttempt(3)).isEqualTo(Duration.ofSeconds(8));
        assertThat(policy.delayForAttempt(4)).isEqualTo(Duration.ofSeconds(10));
        assertThat(policy.delayForAttempt(500)).isEqualTo(Duration.ofSeconds(10));
    }

    @Test void deliveredPath() {
        Instant now = Instant.now();
        OutboxEvent e = ev().markInFlight(now).markDelivered(now);
        assertThat(e.status()).isEqualTo(OutboxStatus.DELIVERED);
        assertThat(e.attempts()).isEqualTo(1);
    }

    @Test void retryableFailureReschedulesWithBackoff() {
        Instant now = Instant.now();
        OutboxEvent e = ev().markInFlight(now).markFailed("boom", true, policy, now);
        assertThat(e.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(e.nextAttemptAt()).isEqualTo(now.plusSeconds(2));
        assertThat(e.lastError()).isEqualTo("boom");
    }

    @Test void permanentFailureGoesStraightToDeadLetter() {
        Instant now = Instant.now();
        assertThat(ev().markInFlight(now).markFailed("bad", false, policy, now).status()).isEqualTo(OutboxStatus.DEAD_LETTER);
    }

    @Test void exhaustedAttemptsDeadLetter() {
        Instant now = Instant.now();
        OutboxEvent e = ev();
        for (int i = 0; i < 3; i++) {
            e = e.markInFlight(now).markFailed("x", true, policy, now);
        }
        assertThat(e.status()).isEqualTo(OutboxStatus.DEAD_LETTER);
        assertThat(e.attempts()).isEqualTo(3);
    }

    @Test void replayOnlyFromDeadLetterAndKeepsIdempotencyKey() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> ev().replay(now)).isInstanceOf(IllegalStateException.class);
        OutboxEvent dead = ev().markInFlight(now).markFailed("x", false, policy, now);
        OutboxEvent replayed = dead.replay(now);
        assertThat(replayed.status()).isEqualTo(OutboxStatus.PENDING);
        assertThat(replayed.attempts()).isZero();
        assertThat(replayed.replayCount()).isEqualTo(1);
        assertThat(replayed.idempotencyKey()).isEqualTo("key");
    }

    @Test void invalidTransitionsRejected() {
        Instant now = Instant.now();
        assertThatThrownBy(() -> ev().markDelivered(now)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ev().markInFlight(now).markInFlight(now)).isInstanceOf(IllegalStateException.class);
    }

    @Test void staleInFlightRecoversToPending() {
        Instant now = Instant.now();
        assertThat(ev().markInFlight(now).recoverStale(now).status()).isEqualTo(OutboxStatus.PENDING);
    }
}
