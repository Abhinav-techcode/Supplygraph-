package com.supplygraph.concurguard.conflict;

import com.supplygraph.concurguard.api.ConflictType;
import com.supplygraph.concurguard.model.IntentId;

import java.util.Objects;
import java.util.Optional;

/**
 * Concrete conflict instance detected deterministically by ConcurGuard.
 */
public record Conflict(
        ConflictId id,
        ConflictType type,
        IntentId primaryIntentId,
        Optional<IntentId> conflictingIntentId,
        String resourceOrEntityId,
        String explanation,
        Severity severity
) {
    public Conflict {
        Objects.requireNonNull(id, "ConflictId must not be null");
        Objects.requireNonNull(type, "ConflictType must not be null");
        Objects.requireNonNull(primaryIntentId, "PrimaryIntentId must not be null");
        Objects.requireNonNull(conflictingIntentId, "ConflictingIntentId optional must not be null");
        Objects.requireNonNull(resourceOrEntityId, "ResourceOrEntityId must not be null");
        Objects.requireNonNull(explanation, "Explanation must not be null");
        Objects.requireNonNull(severity, "Severity must not be null");
    }

    public static Conflict of(ConflictType type, IntentId primary, IntentId conflicting,
                              String resource, String explanation, Severity severity) {
        return new Conflict(ConflictId.generate(), type, primary, Optional.ofNullable(conflicting), resource, explanation, severity);
    }

    public static Conflict unary(ConflictType type, IntentId primary, String resource, String explanation, Severity severity) {
        return new Conflict(ConflictId.generate(), type, primary, Optional.empty(), resource, explanation, severity);
    }
}
