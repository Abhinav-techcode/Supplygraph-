package com.supplygraph.domain.model.outbox;

import java.time.Duration;

/** Exponential backoff with a cap: delay = min(base * 2^(attempt-1), max). */
public record BackoffPolicy(Duration base, Duration max, int maxAttempts) {
    public BackoffPolicy {
        if (base.isNegative() || base.isZero() || max.compareTo(base) < 0) throw new IllegalArgumentException("invalid backoff durations");
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be >= 1");
    }
    public static BackoffPolicy standard() { return new BackoffPolicy(Duration.ofSeconds(2), Duration.ofMinutes(10), 5); }

    public Duration delayForAttempt(int attempt) {
        long shift = Math.min(Math.max(attempt - 1, 0), 30);
        long millis = Math.min(base.toMillis() * (1L << shift), max.toMillis());
        return Duration.ofMillis(millis);
    }
}
