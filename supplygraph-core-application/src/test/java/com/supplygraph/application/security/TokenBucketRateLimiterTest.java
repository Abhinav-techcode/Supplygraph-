package com.supplygraph.application.security;

import com.supplygraph.domain.error.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenBucketRateLimiterTest {

    private TokenBucketRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        // Capacity of 3 tokens, refilling at 1 token per second
        rateLimiter = new TokenBucketRateLimiter(3, 1.0);
    }

    @Test
    void acquire_withinCapacity_succeeds() {
        String client = "CLIENT-1";
        String op = "SIMULATION";

        rateLimiter.acquire(client, op);
        rateLimiter.acquire(client, op);
        rateLimiter.acquire(client, op);

        // 3 tokens consumed out of 3; next call exceeds rate limit
        assertThatThrownBy(() -> rateLimiter.acquire(client, op))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("SIMULATION");
    }

    @Test
    void differentClientsAndOperations_haveIndependentBuckets() {
        rateLimiter.acquire("CLIENT-A", "OPTIMIZATION");
        rateLimiter.acquire("CLIENT-A", "OPTIMIZATION");
        rateLimiter.acquire("CLIENT-A", "OPTIMIZATION");

        // Client A exhausted on OPTIMIZATION
        assertThatThrownBy(() -> rateLimiter.acquire("CLIENT-A", "OPTIMIZATION"))
                .isInstanceOf(RateLimitExceededException.class);

        // Client B has separate bucket
        rateLimiter.acquire("CLIENT-B", "OPTIMIZATION");
        assertThat(rateLimiter.getAvailableTokens("CLIENT-B", "OPTIMIZATION")).isLessThan(3.0);

        // Client A has separate bucket for SIMULATION
        rateLimiter.acquire("CLIENT-A", "SIMULATION");
        assertThat(rateLimiter.getAvailableTokens("CLIENT-A", "SIMULATION")).isLessThan(3.0);
    }
}

