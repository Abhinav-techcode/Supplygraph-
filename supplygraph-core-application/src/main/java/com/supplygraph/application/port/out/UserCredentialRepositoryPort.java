package com.supplygraph.application.port.out;

import java.time.Instant;
import java.util.Optional;

public interface UserCredentialRepositoryPort {
    /** @param lockedUntil null when the account is not locked */
    record Credential(String userId, String passwordHash, int failedAttempts, Instant lockedUntil) {}

    Optional<Credential> find(String userId);
    void savePassword(String userId, String passwordHash);
    void recordFailure(String userId, int failedAttempts, Instant lockedUntil);
    void clearFailures(String userId);
}
