package com.supplygraph.application.security;

import com.supplygraph.domain.error.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe token bucket rate limiter for protecting expensive optimization, simulation,
 * and AI agent operations from resource exhaustion.
 */
@Service
public class TokenBucketRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TokenBucketRateLimiter.class);

    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    // Default configuration: 10 operations per minute for heavy compute, bursting up to 10
    private final int defaultCapacity;
    private final double defaultRefillRatePerSecond;

    public TokenBucketRateLimiter() {
        this(10, 10.0 / 60.0); // 10 operations per minute default
    }

    public TokenBucketRateLimiter(int defaultCapacity, double defaultRefillRatePerSecond) {
        this.defaultCapacity = defaultCapacity;
        this.defaultRefillRatePerSecond = defaultRefillRatePerSecond;
    }

    /**
     * Attempts to acquire 1 token for the specified client and operation.
     * Throws RateLimitExceededException if tokens are exhausted.
     */
    public void acquire(String clientKey, String operationType) {
        String key = clientKey + ":" + operationType;
        TokenBucket bucket = buckets.computeIfAbsent(
                key,
                k -> new TokenBucket(defaultCapacity, defaultRefillRatePerSecond)
        );

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit tripped for client [{}] on operation [{}]", clientKey, operationType);
            throw new RateLimitExceededException(clientKey, operationType);
        }
    }

    /**
     * Checks available token balance without consuming.
     */
    public double getAvailableTokens(String clientKey, String operationType) {
        String key = clientKey + ":" + operationType;
        TokenBucket bucket = buckets.get(key);
        return bucket != null ? bucket.getTokens() : defaultCapacity;
    }

    public void reset() {
        buckets.clear();
    }

    private static class TokenBucket {
        private final int capacity;
        private final double refillRatePerSecond;
        private double tokens;
        private long lastRefillTimestampNanos;

        public TokenBucket(int capacity, double refillRatePerSecond) {
            this.capacity = capacity;
            this.refillRatePerSecond = refillRatePerSecond;
            this.tokens = capacity;
            this.lastRefillTimestampNanos = System.nanoTime();
        }

        public synchronized boolean tryConsume(int count) {
            refill();
            if (tokens >= count) {
                tokens -= count;
                return true;
            }
            return false;
        }

        public synchronized double getTokens() {
            refill();
            return tokens;
        }

        private void refill() {
            long now = System.nanoTime();
            double secondsPassed = (now - lastRefillTimestampNanos) / 1_000_000_000.0;
            if (secondsPassed > 0) {
                tokens = Math.min(capacity, tokens + secondsPassed * refillRatePerSecond);
                lastRefillTimestampNanos = now;
            }
        }
    }
}

